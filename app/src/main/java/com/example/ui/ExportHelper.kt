package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.FamilyMember
import java.io.File
import java.io.FileOutputStream

private class LayoutResult(
    val activeMembers: List<FamilyMember>,
    val positions: Map<String, Pair<Float, Float>>,
    val width: Int,
    val height: Int,
    val activeChildrenMap: Map<Long?, List<FamilyMember>>
)

object ExportHelper {

    fun shareText(context: Context, text: String, title: String = "Share Family Tree JSON") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun computeTreeLayout(
        members: List<FamilyMember>,
        showSon: Boolean,
        showSpouse: Boolean,
        showDaughter: Boolean,
        showBrother: Boolean,
        showSister: Boolean
    ): LayoutResult {
        if (members.isEmpty()) {
            return LayoutResult(emptyList(), emptyMap(), 1000, 800, emptyMap())
        }

        // Recursive configuration filter
        val activeMembers = mutableListOf<FamilyMember>()
        val roots = members.filter { it.parentId == null }
        
        fun addFiltered(parent: FamilyMember) {
            activeMembers.add(parent)
            if (showSon) {
                val sons = members.filter { it.parentId == parent.id }
                val filteredSons = if (showBrother) {
                    sons
                } else {
                    sons.take(1)
                }
                filteredSons.forEach { addFiltered(it) }
            }
        }
        roots.forEach { addFiltered(it) }

        val activeChildrenMap = activeMembers.groupBy { it.parentId }
        val activeRoots = activeMembers.filter { it.parentId == null }
        
        // Node dimensions
        val nodeW = 120f
        val nodeH = 70f
        val horizSpacing = 60f
        val vertSpacing = 80f
        
        val positions = mutableMapOf<String, Pair<Float, Float>>()
        var nextLeafX = 30f
        
        fun layoutNode(member: FamilyMember, depth: Int): Float {
            val y = depth * (nodeH + vertSpacing) + 110f
            
            // Respect the collapsed state `isExpanded` in layout calculation
            val sons = if (member.isExpanded) {
                activeChildrenMap[member.id] ?: emptyList()
            } else {
                emptyList()
            }
            val daughtersList = if (member.isExpanded && showDaughter && member.daughters.isNotEmpty()) {
                member.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            
            val childKeys = mutableListOf<String>()
            val sonKeysMap = mutableMapOf<String, FamilyMember>()
            
            sons.forEach { son ->
                val key = "member_${son.id}"
                childKeys.add(key)
                sonKeysMap[key] = son
            }
            daughtersList.forEachIndexed { idx, _ ->
                val key = "daughter_${member.id}_$idx"
                childKeys.add(key)
            }
            
            val hasSpouse = showSpouse && member.spouse.isNotEmpty()
            val coupledWidth = if (hasSpouse) {
                nodeW * 2 + 15f
            } else {
                nodeW
            }
            
            val x: Float
            if (childKeys.isEmpty()) {
                x = nextLeafX
                positions["member_${member.id}"] = Pair(x, y)
                if (hasSpouse) {
                    positions["spouse_${member.id}"] = Pair(x + nodeW + 15f, y)
                }
                nextLeafX += coupledWidth + horizSpacing
            } else {
                // Track start boundary of children layout to prevent parent overflow/overlap to the left of it
                val startingX = nextLeafX

                val childXs = mutableListOf<Float>()
                childKeys.forEach { childKey ->
                    if (sonKeysMap.containsKey(childKey)) {
                        val son = sonKeysMap[childKey]!!
                        val childX = layoutNode(son, depth + 1)
                        childXs.add(childX)
                    } else {
                        val childX = nextLeafX
                        positions[childKey] = Pair(childX, y + nodeH + vertSpacing)
                        childXs.add(childX)
                        nextLeafX += nodeW + horizSpacing
                    }
                }
                
                val firstChildX = childXs.first()
                val lastChildX = childXs.last()
                
                val centerOfChildren = (firstChildX + lastChildX + nodeW) / 2f
                val calculatedX = centerOfChildren - coupledWidth / 2f
                
                // CRITICAL EXPORT OVERLAP FIX: Force coordinate X to always stay to the right of startingX limit
                x = calculatedX.coerceAtLeast(startingX)
                
                positions["member_${member.id}"] = Pair(x, y)
                if (hasSpouse) {
                    positions["spouse_${member.id}"] = Pair(x + nodeW + 15f, y)
                }
                
                if (x + coupledWidth + horizSpacing > nextLeafX) {
                    nextLeafX = x + coupledWidth + horizSpacing
                }
            }
            return x
        }
        
        activeRoots.forEach { layoutNode(it, 0) }

        var maxX = 1000f
        var maxY = 800f
        positions.forEach { (_, pos) ->
            if (pos.first + nodeW + 100f > maxX) {
                maxX = pos.first + nodeW + 100f
            }
            if (pos.second + nodeH + 100f > maxY) {
                maxY = pos.second + nodeH + 100f
            }
        }
        val width = maxX.toInt().coerceAtLeast(1000)
        val height = maxY.toInt().coerceAtLeast(800)

        return LayoutResult(activeMembers, positions, width, height, activeChildrenMap)
    }

    private fun drawTreeOnCanvas(
        canvas: Canvas,
        layout: LayoutResult,
        showSpouse: Boolean,
        showDaughter: Boolean
    ) {
        val nodeW = 120f
        val nodeH = 70f
        val vertSpacing = 80f
        val activeMembers = layout.activeMembers
        val positions = layout.positions
        val activeChildrenMap = layout.activeChildrenMap
        val width = layout.width

        // Slate Background
        canvas.drawColor(0xFFF1F5F9.toInt())

        val linePaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val cardPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val cardBorderPaint = Paint().apply {
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val namePaint = Paint().apply {
            color = 0xFF1F2937.toInt()
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val yearPaint = Paint().apply {
            color = 0xFF64748B.toInt()
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Draw connections
        activeMembers.forEach { m ->
            val mKey = "member_${m.id}"
            val mPos = positions[mKey] ?: return@forEach
            
            val mx = mPos.first + nodeW / 2f
            val my = mPos.second + nodeH / 2f
            
            val hasSpouse = showSpouse && m.spouse.isNotEmpty()
            val sKey = "spouse_${m.id}"
            val sPos = positions[sKey]
            
            val midParentX: Float
            val midParentY: Float
            
            if (hasSpouse && sPos != null) {
                val sx = sPos.first
                val sy = sPos.second + nodeH / 2f
                
                // Spouse solid line (pink/rose)
                canvas.drawLine(mPos.first + nodeW, my, sx, sy, Paint().apply {
                    color = 0xFFF43F5E.toInt()
                    strokeWidth = 3f
                    isAntiAlias = true
                })
                
                midParentX = (mPos.first + nodeW + sx) / 2f
                midParentY = my
            } else {
                midParentX = mx
                midParentY = mPos.second + nodeH
            }
            
            val sons = if (m.isExpanded) {
                activeChildrenMap[m.id] ?: emptyList()
            } else {
                emptyList()
            }
            val daughtersList = if (m.isExpanded && showDaughter && m.daughters.isNotEmpty()) {
                m.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            
            val totalChildren = sons.size + daughtersList.size
            if (totalChildren > 0) {
                val midY = mPos.second + nodeH + vertSpacing / 2f
                canvas.drawLine(midParentX, midParentY, midParentX, midY, linePaint)
                
                sons.forEach { son ->
                    val sonKey = "member_${son.id}"
                    val sonPos = positions[sonKey]
                    if (sonPos != null) {
                        val cx = sonPos.first + nodeW / 2f
                        val cy = sonPos.second
                        canvas.drawLine(midParentX, midY, cx, midY, linePaint)
                        canvas.drawLine(cx, midY, cx, cy, linePaint)
                    }
                }
                
                daughtersList.forEachIndexed { dIdx, _ ->
                    val dKey = "daughter_${m.id}_$dIdx"
                    val dPos = positions[dKey]
                    if (dPos != null) {
                        val cx = dPos.first + nodeW / 2f
                        val cy = dPos.second
                        canvas.drawLine(midParentX, midY, cx, midY, linePaint)
                        canvas.drawLine(cx, midY, cx, cy, linePaint)
                    }
                }
            }
        }

        // Draw card nodes
        positions.forEach { (key, pos) ->
            val (x, y) = pos
            val rect = RectF(x, y, x + nodeW, y + nodeH)
            
            if (key.startsWith("member_")) {
                val mId = key.substringAfter("member_").toLongOrNull() ?: return@forEach
                val m = activeMembers.firstOrNull { it.id == mId } ?: return@forEach
                
                cardPaint.color = 0xFFFFFFFF.toInt()
                cardBorderPaint.color = 0xFF24B1B1.toInt()
                canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
                
                canvas.drawText("#${String.format("%04d", m.id)}", x + 15f, y + 20f, Paint().apply {
                    color = 0xFF94A3B8.toInt()
                    textSize = 9f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                })
                
                var dispName = m.name
                if (dispName.length > 18) dispName = dispName.take(15) + "..."
                canvas.drawText(dispName, x + 15f, y + 43f, namePaint)
                
                val subText = if (m.birthYear.isNotEmpty()) "Born ${m.birthYear}" else "Gen ${m.generation}"
                canvas.drawText(subText, x + 15f, y + 68f, yearPaint)
                
            } else if (key.startsWith("spouse_")) {
                val mId = key.substringAfter("spouse_").toLongOrNull() ?: return@forEach
                val m = activeMembers.firstOrNull { it.id == mId } ?: return@forEach
                
                cardPaint.color = 0xFFFFF1F2.toInt()
                cardBorderPaint.color = 0xFFFDA4AF.toInt()
                canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
                
                canvas.drawText("💍 WIFE / هاوسەر", x + 15f, y + 20f, Paint().apply {
                    color = 0xFFE11D48.toInt()
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })
                
                var dispSpouse = m.spouse
                if (dispSpouse.length > 18) dispSpouse = dispSpouse.take(15) + "..."
                canvas.drawText(dispSpouse, x + 15f, y + 43f, Paint().apply {
                    color = 0xFF881337.toInt()
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })
                
                canvas.drawText("Wife of ${m.name}", x + 15f, y + 68f, Paint().apply {
                    color = 0xFF9F1239.toInt()
                    textSize = 9f
                })
                
            } else if (key.startsWith("daughter_")) {
                val parts = key.split("_")
                val parentId = parts[1].toLongOrNull() ?: return@forEach
                val dIdx = parts[2].toIntOrNull() ?: return@forEach
                val parent = activeMembers.firstOrNull { it.id == parentId } ?: return@forEach
                val daughtersList = parent.daughters.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val daughterName = daughtersList.getOrNull(dIdx) ?: "Daughter"
                
                cardPaint.color = 0xFFF5F3FF.toInt()
                cardBorderPaint.color = 0xFFC7D2FE.toInt()
                canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
                
                canvas.drawText("👧 DAUGHTER / کچ", x + 15f, y + 20f, Paint().apply {
                    color = 0xFF6D28D9.toInt()
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })
                
                var dispDaughter = daughterName
                if (dispDaughter.length > 18) dispDaughter = dispDaughter.take(15) + "..."
                canvas.drawText(dispDaughter, x + 15f, y + 43f, Paint().apply {
                    color = 0xFF4C1D95.toInt()
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })
                
                canvas.drawText("Daughter of ${parent.name}", x + 15f, y + 68f, Paint().apply {
                    color = 0xFF5B21B6.toInt()
                    textSize = 9f
                })
            }
        }

        // Header ribbon
        val headerRibbon = Paint().apply {
            color = 0xFF24B1B1.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 64f, headerRibbon)
        
        val titlePaintInside = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Family Kurd — Customized Family Lineage Map Diagram", 35f, 38f, titlePaintInside)
    }

    fun sharePdf(
        context: Context,
        members: List<FamilyMember>,
        showSon: Boolean,
        showSpouse: Boolean,
        showDaughter: Boolean,
        showBrother: Boolean,
        showSister: Boolean
    ) {
        if (members.isEmpty()) return

        val layout = computeTreeLayout(
            members = members,
            showSon = showSon,
            showSpouse = showSpouse,
            showDaughter = showDaughter,
            showBrother = showBrother,
            showSister = showSister
        )

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(layout.width, layout.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        
        drawTreeOnCanvas(
            canvas = page.canvas,
            layout = layout,
            showSpouse = showSpouse,
            showDaughter = showDaughter
        )
        pdfDocument.finishPage(page)

        try {
            val file = File(context.cacheDir, "family_kurd_tree.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(context, "com.aistudio.familykurd.txrmbz.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Family Kurd Lineage PDF Map")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Family Tree PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareTreeImage(
        context: Context,
        members: List<FamilyMember>,
        showSon: Boolean,
        showSpouse: Boolean,
        showDaughter: Boolean,
        showBrother: Boolean,
        showSister: Boolean
    ) {
        if (members.isEmpty()) return

        val layout = computeTreeLayout(
            members = members,
            showSon = showSon,
            showSpouse = showSpouse,
            showDaughter = showDaughter,
            showBrother = showBrother,
            showSister = showSister
        )

        val bitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawTreeOnCanvas(
            canvas = canvas,
            layout = layout,
            showSpouse = showSpouse,
            showDaughter = showDaughter
        )

        try {
            val file = File(context.cacheDir, "family_kurd_tree.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()

            val uri = FileProvider.getUriForFile(context, "com.aistudio.familykurd.txrmbz.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Family Kurd Lineage Image")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Family Tree JPG"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
