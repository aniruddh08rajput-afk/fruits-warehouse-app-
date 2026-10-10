
package com.example.fruitswarehouse

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FruitsWarehouseApp() } }
    }
}

data class StockLot(
    val lot: String, val productCode: String, val fruit: String, val brand: String,
    val count: String, val size: String, val kgPerCarton: Double,
    val room: String, val block: String, val cartons: Int, val pallets: Int = 0
) {
    val totalKg: Double get() = cartons * kgPerCarton
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitsWarehouseApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fruit_warehouse", 0) }
    var lots by remember { mutableStateOf(loadLots(prefs.getString("lots", "[]") ?: "[]")) }
    var tab by remember { mutableStateOf("Stock") }
    var message by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf<StockLot?>(null) }
    var moveType by remember { mutableStateOf("Inward") }
    var moveQty by remember { mutableStateOf("1") }
    var countLot by remember { mutableStateOf<StockLot?>(null) }
    var physicalQty by remember { mutableStateOf("") }
    var softwareQty by remember { mutableStateOf("") }

    fun persist(newLots: List<StockLot>) {
        lots = newLots
        prefs.edit().putString("lots", toJson(newLots).toString()).apply()
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(toCsv(lots)) }
            message = "CSV export तैयार है। Excel में खोल सकते हैं।"
        } catch (e: Exception) { message = "Export असफल: ${e.message}" }
    }
    
onImport = { importLauncher.launch(arrayOf("*/*")) } context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val imported = parseCsv(text)
            if (imported.isEmpty()) message = "कोई वैध रिकॉर्ड नहीं मिला। CSV का header सही रखें।"
            else {
                val existingIds = lots.map { it.lot }.toSet()
                val fresh = imported.filter { it.lot !in existingIds }
                persist(lots + fresh)
                message = "${fresh.size} नए Lot आयात हुए; duplicate Lot Number छोड़े गए।"
            }
        } catch (e: Exception) { message = "Import असफल: ${e.message}" }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fruits Warehouse", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = { if (tab == "Stock") FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "Add lot") } },
        bottomBar = {
            NavigationBar {
                listOf("Stock", "Counting", "Reports").forEach { name ->
                    NavigationBarItem(selected = tab == name, onClick = { tab = name }, icon = {
                        Icon(when (name) { "Stock" -> Icons.Default.Inventory2; "Counting" -> Icons.Default.Warehouse; else -> Icons.Default.FileDownload }, contentDescription = name)
                    }, label = { Text(name) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (message.isNotBlank()) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f)); TextButton(onClick = { message = "" }) { Text("ठीक") }
                    }
                }
            }
            when (tab) {
                "Stock" -> StockScreen(lots, onInward = { showMove = it; moveType = "Inward"; moveQty = "1" }, onOutward = { showMove = it; moveType = "Outward"; moveQty = "1" }, onCount = { countLot = it; physicalQty = it.cartons.toString(); softwareQty = it.cartons.toString() })
                "Counting" -> CountingScreen(lots, onCount = { countLot = it; physicalQty = ""; softwareQty = it.cartons.toString() })
                else -> ReportsScreen(lots, onExport = { exportLauncher.launch("fruits_warehouse_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.csv") }, onImport = { importLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/csv")) })
            }
        }
    }

    if (showAdd) AddLotDialog(onDismiss = { showAdd = false }, onSave = { lot ->
        if (lots.any { it.lot.equals(lot.lot, true) }) message = "यह Lot Number पहले से मौजूद है।"
        else { persist(lots + lot); message = "Opening Stock सेव हुआ।" }
        showAdd = false
    })

    showMove?.let { lot ->
        AlertDialog(onDismissRequest = { showMove = null }, title = { Text("$moveType — ${lot.lot}") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("वर्तमान Carton: ${lot.cartons}")
                OutlinedTextField(moveQty, { moveQty = it.filter(Char::isDigit) }, label = { Text("Carton संख्या") }, singleLine = true)
                Text("एक ही Lot में Brand और Count नहीं बदलें।", style = MaterialTheme.typography.bodySmall)
            } }, confirmButton = { TextButton(onClick = {
                val q = moveQty.toIntOrNull() ?: 0
                if (q <= 0) message = "Carton संख्या 1 या अधिक होनी चाहिए।"
                else if (moveType == "Outward" && q > lot.cartons) message = "Outward उपलब्ध स्टॉक से अधिक है।"
                else {
                    val updated = lots.map { if (it.lot == lot.lot) it.copy(cartons = it.cartons + if (moveType == "Inward") q else -q) else it }
                    persist(updated); message = "$moveType दर्ज हुआ: $q Carton"; showMove = null
                }
            }) { Text("पुष्टि करें") } }, dismissButton = { TextButton(onClick = { showMove = null }) { Text("रद्द") } })
    }

    countLot?.let { lot ->
        AlertDialog(onDismissRequest = { countLot = null }, title = { Text("Physical Counting — ${lot.lot}") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Software/Book Stock: ${lot.cartons} Carton")
                OutlinedTextField(softwareQty, { softwareQty = it.filter(Char::isDigit) }, label = { Text("Software Stock (Carton)") }, singleLine = true)
                OutlinedTextField(physicalQty, { physicalQty = it.filter(Char::isDigit) }, label = { Text("वास्तविक गिनती (Carton)") }, singleLine = true)
                val diff = (physicalQty.toIntOrNull() ?: 0) - (softwareQty.toIntOrNull() ?: 0)
                Text("अंतर: ${if (diff > 0) "+" else ""}$diff Carton", fontWeight = FontWeight.Bold)
                Text("यह केवल अंतर दिखाता है; स्टॉक अपने आप नहीं बदलेगा।", style = MaterialTheme.typography.bodySmall)
            } }, confirmButton = { TextButton(onClick = { message = "Counting दर्ज: ${lot.lot}, अंतर ${(physicalQty.toIntOrNull() ?: 0) - (softwareQty.toIntOrNull() ?: 0)} Carton. स्टॉक बदला नहीं गया।"; countLot = null }) { Text("गिनती सेव करें") } }, dismissButton = { TextButton(onClick = { countLot = null }) { Text("रद्द") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(lots: List<StockLot>, onInward: (StockLot) -> Unit, onOutward: (StockLot) -> Unit, onCount: (StockLot) -> Unit) {
    val totalCartons = lots.sumOf { it.cartons }
    val totalKg = lots.sumOf { it.totalKg }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("कुल Carton", totalCartons.toString(), Modifier.weight(1f))
            SummaryCard("कुल वजन", "${"%.1f".format(totalKg)} kg", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text("Lot-wise Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (lots.isEmpty()) Text("अभी कोई Lot नहीं। + दबाकर Opening Stock जोड़ें।", Modifier.padding(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lots, key = { it.lot }) { lot ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("LOT: ${lot.lot}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("${lot.fruit} • ${lot.productCode} • ${lot.brand}")
                        Text("Count ${lot.count} | Size ${lot.size} | ${lot.kgPerCarton} kg/Carton")
                        Text("Room ${lot.room} / Block ${lot.block} | ${lot.cartons} Carton | ${"%.1f".format(lot.totalKg)} kg")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { onInward(lot) }) { Text("Inward +") }
                            TextButton(onClick = { onOutward(lot) }) { Text("Outward −") }
                            TextButton(onClick = { onCount(lot) }) { Text("Count") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.bodySmall); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
}

@Composable
fun CountingScreen(lots: List<StockLot>, onCount: (StockLot) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Physical Stock Counting", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Lot चुनें, वास्तविक Carton गिनें और अंतर दर्ज करें। इस शुरुआती संस्करण में Photo AI अभी शामिल नहीं है।", Modifier.padding(vertical = 8.dp))
        LazyColumn { items(lots, key = { it.lot }) { lot -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(lot.lot, fontWeight = FontWeight.Bold); Text("${lot.brand} • Count ${lot.count} • Book ${lot.cartons}") }
                Button(onClick = { onCount(lot) }) { Text("गिनें") }
            }
        } } }
    }
}

@Composable
fun ReportsScreen(lots: List<StockLot>, onExport: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reports & Excel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SummaryCard("कुल Lots", lots.size.toString(), Modifier.fillMaxWidth())
        SummaryCard("कुल Carton", lots.sumOf { it.cartons }.toString(), Modifier.fillMaxWidth())
        SummaryCard("कुल वजन", "${"%.1f".format(lots.sumOf { it.totalKg })} kg", Modifier.fillMaxWidth())
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("CSV / Excel से Import") }
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("CSV Export (Excel में खुलेगा)") }
        Text("CSV template header: lot,productCode,fruit,brand,count,size,kgPerCarton,room,block,cartons,pallets", style = MaterialTheme.typography.bodySmall)
        Text("ध्यान दें: यह MVP CSV फाइल आयात करता है; .xlsx workbook सीधे पढ़ने के लिए अगला संस्करण चाहिए। Import में पहले से मौजूद Lot Number duplicate होने पर छोड़ दिया जाता है।", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AddLotDialog(onDismiss: () -> Unit, onSave: (StockLot) -> Unit) {
    var lot by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
    var fruit by remember { mutableStateOf("Apple") }; var brand by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("") }; var size by remember { mutableStateOf("") }
    var kg by remember { mutableStateOf("10") }; var room by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("") }; var cartons by remember { mutableStateOf("0") }
    var pallets by remember { mutableStateOf("0") }; var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Opening Stock / नया Lot") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(lot, { lot = it }, label = { Text("Lot Number *") }, singleLine = true)
            OutlinedTextField(code, { code = it }, label = { Text("Product Code *") }, singleLine = true)
            OutlinedTextField(fruit, { fruit = it }, label = { Text("Fruit Name *") }, singleLine = true)
            OutlinedTextField(brand, { brand = it }, label = { Text("Brand *") }, singleLine = true)
            OutlinedTextField(count, { count = it }, label = { Text("Count * (जैसे 80)") }, singleLine = true)
            OutlinedTextField(size, { size = it }, label = { Text("Size / Calibre *") }, singleLine = true)
            OutlinedTextField(kg, { kg = it }, label = { Text("Weight per Carton (kg) *") }, singleLine = true)
            OutlinedTextField(room, { room = it }, label = { Text("Room *") }, singleLine = true)
            OutlinedTextField(block, { block = it }, label = { Text("Block *") }, singleLine = true)
            OutlinedTextField(cartons, { cartons = it.filter(Char::isDigit) }, label = { Text("Opening Cartons") }, singleLine = true)
            OutlinedTextField(pallets, { pallets = it.filter(Char::isDigit) }, label = { Text("Pallets (optional)") }, singleLine = true)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        } }, confirmButton = { TextButton(onClick = {
            val weight = kg.toDoubleOrNull(); val qty = cartons.toIntOrNull(); val pal = pallets.toIntOrNull() ?: 0
            if (listOf(lot, code, fruit, brand, count, size, room, block).any { it.isBlank() } || weight == null || weight <= 0 || qty == null || qty < 0) error = "सभी * फ़ील्ड सही भरें।"
            else onSave(StockLot(lot.trim(), code.trim(), fruit.trim(), brand.trim(), count.trim(), size.trim(), weight, room.trim(), block.trim(), qty, pal))
        }) { Text("सेव करें") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द") } })
}

fun toJson(lots: List<StockLot>): JSONArray = JSONArray().apply { lots.forEach { l -> put(JSONObject().apply {
    put("lot", l.lot); put("productCode", l.productCode); put("fruit", l.fruit); put("brand", l.brand); put("count", l.count); put("size", l.size); put("kgPerCarton", l.kgPerCarton); put("room", l.room); put("block", l.block); put("cartons", l.cartons); put("pallets", l.pallets)
}) } }
fun loadLots(raw: String): List<StockLot> = try { val arr = JSONArray(raw); (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); StockLot(o.getString("lot"), o.getString("productCode"), o.getString("fruit"), o.getString("brand"), o.getString("count"), o.getString("size"), o.getDouble("kgPerCarton"), o.getString("room"), o.getString("block"), o.getInt("cartons"), o.optInt("pallets", 0)) } } catch (_: Exception) { emptyList() }
fun toCsv(lots: List<StockLot>): String {
    val header = "lot,productCode,fruit,brand,count,size,kgPerCarton,room,block,cartons,pallets"
    return (listOf(header) + lots.map { listOf(it.lot,it.productCode,it.fruit,it.brand,it.count,it.size,it.kgPerCarton.toString(),it.room,it.block,it.cartons.toString(),it.pallets.toString()).joinToString(",") { v -> "\"${v.replace("\"", "\"\"")}\"" } }).joinToString("\n")
}
fun parseCsv(text: String): List<StockLot> {
    val lines = text.trim().lines().dropWhile { it.isBlank() }
    if (lines.size < 2) return emptyList()
    fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>(); val cur = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) { val c = line[i]; if (c == '"') { if (quoted && i + 1 < line.length && line[i+1] == '"') { cur.append('"'); i++ } else quoted = !quoted } else if (c == ',' && !quoted) { out.add(cur.toString()); cur.setLength(0) } else cur.append(c); i++ }
        out.add(cur.toString()); return out
    }
    val header = splitCsv(lines.first()).map { it.trim().lowercase() }
    val idx = header.withIndex().associate { it.value to it.index }
    fun field(row: List<String>, name: String, default: String = "") = row.getOrNull(idx[name] ?: -1)?.trim().orEmpty().ifBlank { default }
    return lines.drop(1).mapNotNull { line -> try {
        val r = splitCsv(line); val lot = field(r,"lot"); if (lot.isBlank()) return@mapNotNull null
        StockLot(lot, field(r,"productcode"), field(r,"fruit"), field(r,"brand"), field(r,"count"), field(r,"size"), field(r,"kgpercarton","10").toDouble(), field(r,"room","Unassigned"), field(r,"block","Unassigned"), field(r,"cartons","0").toInt(), field(r,"pallets","0").toInt())
    } catch (_: Exception) { null } }
}
