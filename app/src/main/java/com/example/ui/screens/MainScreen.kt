package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityEntity
import com.example.data.ChatMessageEntity
import com.example.data.TransactionEntity
import com.example.ui.components.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.FinanceActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    viewModel: FinanceActivityViewModel,
    onLogout: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val authState by viewModel.authState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        GlassmorphicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // High-fidelity dynamic header with blur feel
            TopHeaderBar(viewModel, authState)

            // Animated Screen transition based on state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    AppTab.FINANCE -> FinanceLedgerScreen(viewModel)
                    AppTab.CALENDAR -> CalendarPlannerScreen(viewModel)
                    AppTab.CHAT -> ChatCoreScreen(viewModel)
                    AppTab.ANALYTICS -> AnalyticsDashboardScreen(viewModel)
                    AppTab.PROFILE -> ProfileSettingsScreen(viewModel, onLogout)
                }
            }

            // Fixed Glassmorphic 5-Tab Navigation Bar at bottom
            GlassmorphicNavigationBar(viewModel, currentTab)
        }
    }
}

// 1. TOP HEADER BAR
@Composable
fun TopHeaderBar(
    viewModel: FinanceActivityViewModel,
    authState: com.example.viewmodel.AuthState
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val syncLabel = if (authState.isSandboxMode) Localization.get("sandbox_local", appLang) else Localization.get("cloud_synced", appLang)
    val syncIcon = if (authState.isSandboxMode) Icons.Default.Warning else Icons.Default.CheckCircle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = Localization.get("hello", appLang).format(authState.displayName ?: "Partner"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = syncIcon,
                    contentDescription = null,
                    tint = if (authState.isSandboxMode) Color.White.copy(alpha = 0.5f) else Color.Green,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = syncLabel,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Feature status indicators (mini icons)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isTts by viewModel.isTtsEnabled.collectAsState()
            val isSearch by viewModel.isSearchGroundingEnabled.collectAsState()

            if (isTts) {
                Surface(
                    shape = CircleShape,
                    color = VividViolet.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = Localization.get("voice_active", appLang),
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
            if (isSearch) {
                Surface(
                    shape = CircleShape,
                    color = VividIndigo.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = Localization.get("search_active", appLang),
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

// 2. TAB 1: FINANCE LEDGER & MANUAL INPUT
@Composable
fun FinanceLedgerScreen(viewModel: FinanceActivityViewModel) {
    val appLang by viewModel.appLanguage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val customCats by viewModel.customCategories.collectAsState()
    val recurringTxList by viewModel.recurringTransactions.collectAsState()
    val dynamicAttrs by viewModel.dynamicAttributes.collectAsState()

    // Standard Form Inputs
    var merchant by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food/Beverage") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    // Dynamic attribute values
    val dynamicValues = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(dynamicAttrs) {
        dynamicAttrs.forEach { attr ->
            if (!dynamicValues.containsKey(attr.name)) {
                dynamicValues[attr.name] = ""
            }
        }
    }

    val defaultCategories = listOf("Food/Beverage", "Transportation", "Education", "Entertainment", "Shopping", "Salary", "Other")
    val allCategories = defaultCategories + customCats.map { it.name }
    val paymentMethods = listOf("Cash", "Card", "E-Wallet", "Other")

    // Custom Field creator input
    var newFieldName by remember { mutableStateOf("") }
    var showNewFieldInput by remember { mutableStateOf(false) }

    // Custom Category management inputs
    var newCatName by remember { mutableStateOf("") }
    var newCatParent by remember { mutableStateOf("") }
    var showCategoryManager by remember { mutableStateOf(false) }

    // Recurring Transaction inputs
    var showRecurringSetup by remember { mutableStateOf(false) }
    var recMerchant by remember { mutableStateOf("") }
    var recAmountStr by remember { mutableStateOf("") }
    var recCategory by remember { mutableStateOf("Food/Beverage") }
    var recPaymentMethod by remember { mutableStateOf("Cash") }
    var recFrequency by remember { mutableStateOf("Monthly") }
    var recNote by remember { mutableStateOf("") }
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manual Input Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Localization.get("add_transaction_title", appLang),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text(Localization.get("merchant_label", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(Localization.get("amount_label", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Category Selection Representation
                        Box(modifier = Modifier.weight(1f)) {
                            var categoryExpanded by remember { mutableStateOf(false) }
                            Button(
                                onClick = { categoryExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(category, color = Color.White, fontSize = 12.sp, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E1B4B))
                            ) {
                                allCategories.forEach { cat ->
                                    val isCustom = cat !in defaultCategories
                                    val subText = if (isCustom) {
                                        val match = customCats.firstOrNull { it.name == cat }
                                        if (match?.parentCategory != null) " (Sub: ${match.parentCategory})" else " (Custom)"
                                    } else ""
                                    DropdownMenuItem(
                                        text = { Text("$cat$subText", color = Color.White) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Payment Method Selection
                        Box(modifier = Modifier.weight(1f)) {
                            var payExpanded by remember { mutableStateOf(false) }
                            Button(
                                onClick = { payExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(paymentMethod, color = Color.White, fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = payExpanded,
                                onDismissRequest = { payExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E1B4B))
                            ) {
                                paymentMethods.forEach { pm ->
                                    DropdownMenuItem(
                                        text = { Text(pm, color = Color.White) },
                                        onClick = {
                                            paymentMethod = pm
                                            payExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(Localization.get("note_label", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // --- DYNAMIC ADAPTIVE FIELDS ---
                    if (dynamicAttrs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.get("dynamic_attrs", appLang),
                            color = BentoCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                        dynamicAttrs.forEach { attr ->
                            val currentVal = dynamicValues[attr.name] ?: ""
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { dynamicValues[attr.name] = it },
                                label = { Text(attr.name, color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VividViolet,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.deleteDynamicAttribute(attr.name) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete attribute", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Define custom attribute on-the-fly directly inside the form
                    Spacer(modifier = Modifier.height(8.dp))
                    if (showNewFieldInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newFieldName,
                                onValueChange = { newFieldName = it },
                                label = { Text(Localization.get("attr_name_placeholder", appLang), color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VividViolet,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newFieldName.isNotEmpty()) {
                                        viewModel.addDynamicAttribute(newFieldName)
                                        newFieldName = ""
                                        showNewFieldInput = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+")
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { showNewFieldInput = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = BentoCyan)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add custom field", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("add_custom_field", appLang), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Glowing Add Button
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (merchant.isNotEmpty() && amt > 0.0) {
                                // Gather dynamic values
                                val combinedNotesList = mutableListOf<String>()
                                if (note.isNotEmpty()) combinedNotesList.add(note)
                                dynamicValues.forEach { (key, value) ->
                                    if (value.isNotEmpty()) {
                                        combinedNotesList.add("$key: $value")
                                    }
                                }
                                val finalNote = combinedNotesList.joinToString(" | ")

                                viewModel.addManualTransaction(category, merchant, amt, paymentMethod, System.currentTimeMillis(), finalNote)
                                
                                // Reset
                                merchant = ""
                                amountStr = ""
                                note = ""
                                dynamicValues.keys.forEach { k -> dynamicValues[k] = "" }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(VividViolet, VividIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(Localization.get("save_transaction", appLang), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- CUSTOM CATEGORY MANAGER SECTION ---
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏷️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("manage_categories_title", appLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { showCategoryManager = !showCategoryManager }) {
                            Icon(
                                if (showCategoryManager) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Manager",
                                tint = Color.White
                            )
                        }
                    }

                    if (showCategoryManager) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(Localization.get("create_cat_sub", appLang), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            label = { Text(Localization.get("new_cat_name", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(Localization.get("parent_cat_optional", appLang), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        
                        var parentExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Button(
                                onClick = { parentExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (newCatParent.isEmpty()) Localization.get("none_parent_option", appLang) else newCatParent, color = Color.White, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false },
                                modifier = Modifier.background(Color(0xFF0F172A))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(Localization.get("none_parent_option", appLang), color = Color.White) },
                                    onClick = {
                                        newCatParent = ""
                                        parentExpanded = false
                                    }
                                )
                                (defaultCategories + customCats.filter { it.parentCategory == null }.map { it.name }).distinct().forEach { parent ->
                                    DropdownMenuItem(
                                        text = { Text(parent, color = Color.White) },
                                        onClick = {
                                            newCatParent = parent
                                            parentExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newCatName.isNotEmpty()) {
                                    viewModel.addCustomCategory(newCatName, if (newCatParent.isEmpty()) null else newCatParent)
                                    newCatName = ""
                                    newCatParent = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Localization.get("create_cat_button", appLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (customCats.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(Localization.get("your_custom_cats", appLang), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            customCats.forEach { cCat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cCat.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (cCat.parentCategory != null) {
                                            Text(Localization.get("subcategory_of", appLang).format(cCat.parentCategory), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                        } else {
                                            Text(Localization.get("main_category", appLang), color = BentoCyan, fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomCategory(cCat.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Custom Category", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- AUTOMATED RECURRING TRANSACTIONS SECTION ---
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔄", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("recurring_title", appLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { showRecurringSetup = !showRecurringSetup }) {
                            Icon(
                                if (showRecurringSetup) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Setup",
                                tint = Color.White
                            )
                        }
                    }

                    if (showRecurringSetup) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(Localization.get("recurring_subtitle", appLang), color = BentoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = recMerchant,
                            onValueChange = { recMerchant = it },
                            label = { Text(Localization.get("recurring_merchant", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = recAmountStr,
                            onValueChange = { recAmountStr = it },
                            label = { Text(Localization.get("recurring_amount", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                var rCatExpanded by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { rCatExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(recCategory, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = rCatExpanded,
                                    onDismissRequest = { rCatExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E1B4B))
                                ) {
                                    allCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = Color.White) },
                                            onClick = {
                                                recCategory = cat
                                                rCatExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                var freqExpanded by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { freqExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(recFrequency, color = Color.White, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = freqExpanded,
                                    onDismissRequest = { freqExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E1B4B))
                                ) {
                                    frequencies.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f, color = Color.White) },
                                            onClick = {
                                                recFrequency = f
                                                freqExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = recNote,
                            onValueChange = { recNote = it },
                            label = { Text(Localization.get("recurring_note", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val amt = recAmountStr.toDoubleOrNull() ?: 0.0
                                if (recMerchant.isNotEmpty() && amt > 0.0) {
                                    val now = System.currentTimeMillis()
                                    val oneYearLater = now + (365L * 24L * 60L * 60L * 1000L * 5L)
                                    viewModel.addRecurringTransaction(
                                        amount = amt,
                                        category = recCategory,
                                        merchant = recMerchant,
                                        paymentMethod = recPaymentMethod,
                                        note = recNote,
                                        frequency = recFrequency,
                                        startDate = now,
                                        endDate = oneYearLater
                                    )
                                    recMerchant = ""
                                    recAmountStr = ""
                                    recNote = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(listOf(VividViolet, VividIndigo))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(Localization.get("recurring_button", appLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (recurringTxList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(Localization.get("active_schedules", appLang), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            recurringTxList.forEach { recPlan ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recPlan.merchant, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Rp ${String.format("%,.0f", recPlan.amount)} • ${recPlan.frequency} • ${recPlan.category}",
                                            color = BentoCyan,
                                            fontSize = 11.sp
                                        )
                                        if (recPlan.note.isNotEmpty()) {
                                            Text(recPlan.note, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteRecurringTransaction(recPlan.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Cancel Recurring Schedule", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Title List Header
        item {
            Text(
                text = Localization.get("recent_transactions", appLang),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = Localization.get("no_transactions", appLang),
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(transactions) { tx ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // High contrast custom emoji representation
                            val categoryEmoji = when (tx.category) {
                                "Food/Beverage" -> "🍔"
                                "Transportation" -> "🚗"
                                "Education" -> "🎓"
                                "Entertainment" -> "🍿"
                                "Shopping" -> "🛍️"
                                "Salary" -> "💸"
                                "Other" -> "🏷️"
                                else -> "📦"
                            }
                            Text(categoryEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(tx.merchant, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (tx.note.isNotEmpty()) {
                                    Text(tx.note, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                Text(
                                    text = "${tx.category} • ${tx.paymentMethod}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (tx.category == "Salary") "+Rp ${String.format("%,.0f", tx.amount)}" else "Rp ${String.format("%,.0f", tx.amount)}",
                                color = if (tx.category == "Salary") Color.Green else VividRose.copy(green = 0.6f, blue = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. TAB 2: CALENDAR PLANNER & MANUAL INPUT
@Composable
fun CalendarPlannerScreen(viewModel: FinanceActivityViewModel) {
    val appLang by viewModel.appLanguage.collectAsState()
    val activities by viewModel.activities.collectAsState()

    var description by remember { mutableStateOf("") }
    var contextMood by remember { mutableStateOf("") }
    var activityType by remember { mutableStateOf("appointment") }
    var durationStr by remember { mutableStateOf("30") }

    val activityTypes = listOf("appointment", "task", "meeting", "habit", "other")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manual Input Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Localization.get("schedule_new_event", appLang),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(Localization.get("event_description", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contextMood,
                            onValueChange = { contextMood = it },
                            label = { Text(Localization.get("context_mood", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )

                        OutlinedTextField(
                            value = durationStr,
                            onValueChange = { durationStr = it },
                            label = { Text(Localization.get("duration", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type Picker dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var typeExpanded by remember { mutableStateOf(false) }
                        Button(
                            onClick = { typeExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Localization.get("activity_type", appLang).format(activityType.uppercase()), color = Color.White, fontSize = 13.sp)
                        }
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E1B4B))
                        ) {
                            activityTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.uppercase(), color = Color.White) },
                                    onClick = {
                                        activityType = type
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Glowing Add Button
                    Button(
                        onClick = {
                            val duration = durationStr.toIntOrNull() ?: 30
                            if (description.isNotEmpty()) {
                                viewModel.addManualActivity(description, System.currentTimeMillis(), contextMood, activityType, duration)
                                description = ""
                                contextMood = ""
                                durationStr = "30"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(VividViolet, VividIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(Localization.get("schedule_activity", appLang), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Timeline Header
        item {
            Text(
                text = Localization.get("planner_timeline", appLang),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (activities.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = Localization.get("no_schedules", appLang),
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(activities) { act ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Icons mapping based on type
                            val typeIcon = when (act.type) {
                                "appointment" -> "💼"
                                "meeting" -> "🗣️"
                                "task" -> "📝"
                                "habit" -> "🌟"
                                else -> "📅"
                            }
                            Text(typeIcon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(act.description, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(act.timestamp))
                                    Text(
                                        text = "$dateStr • ${act.durationMinutes} mins",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (act.context.isNotEmpty()) {
                                    Text(
                                        text = "Context/Mood: ${act.context}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.deleteActivity(act.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoSummarySection(viewModel: FinanceActivityViewModel) {
    val appLang by viewModel.appLanguage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val briefing by viewModel.morningBriefing.collectAsState()

    // Calculate today's spend
    val todayStart = remember(transactions) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todaySpend = remember(transactions, todayStart) {
        transactions.filter { it.timestamp >= todayStart && it.category != "Salary" }.sumOf { it.amount }
    }
    
    // Dynamic formatting of today's spend
    val spendText = remember(todaySpend) {
        if (todaySpend >= 1_000_000) {
            String.format(Locale.US, "%.1fM", todaySpend / 1_000_000.0)
        } else if (todaySpend >= 1_000) {
            String.format(Locale.US, "%.1fK", todaySpend / 1_000.0)
        } else {
            String.format(Locale.US, "%.0f", todaySpend)
        }
    }

    // Calculate Impulse Risk based on non-essential categories (Food/Beverage, Entertainment, Shopping)
    val nonEssentialCount = remember(transactions, todayStart) {
        transactions.count { 
            it.timestamp >= todayStart && 
            (it.category == "Food/Beverage" || it.category == "Entertainment" || it.category == "Shopping") 
        }
    }
    val impulseRisk = remember(nonEssentialCount) {
        when {
            nonEssentialCount >= 3 -> "High"
            nonEssentialCount >= 2 -> "Medium"
            else -> "Low"
        }
    }
    val impulseRiskColor = remember(impulseRisk) {
        when (impulseRisk) {
            "High" -> Color(0xFFFB7185) // rose-400
            "Medium" -> Color(0xFFFBBF24) // amber-400
            else -> Color(0xFF34D399) // emerald-400
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Morning Briefing (Full width)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    // Pulse dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF34D399), CircleShape)
                    )
                    Text(
                        text = Localization.get("morning_briefing_header", appLang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399), // Emerald 400
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = briefing ?: Localization.get("morning_briefing_desc", appLang),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }

        // Row 2: Spend & Risk (Grid-cols-2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Spend card
            GlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = Localization.get("todays_spend_header", appLang),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rp $spendText",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // Impulse Risk card
            GlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = Localization.get("impulse_risk_header", appLang),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Localization.get(impulseRisk.lowercase(), appLang),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = impulseRiskColor,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
}

// 4. TAB 3: GEMINI AI CHAT CORE
@Composable
fun ChatCoreScreen(viewModel: FinanceActivityViewModel) {
    val appLang by viewModel.appLanguage.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val aiName by viewModel.aiName.collectAsState()
    val listState = rememberLazyListState()

    // Auto scroll chat to bottom when message list expands
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggles Controls Bar (Glassmorphic bar at the top of the chat panel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TTS voice reply toggle
            val isTts by viewModel.isTtsEnabled.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Localization.get("tts_label", appLang), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = isTts,
                    onCheckedChange = { viewModel.setTtsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BentoIndigo,
                        checkedTrackColor = BentoIndigo.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }

            // Google Search grounding toggle
            val isSearch by viewModel.isSearchGroundingEnabled.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Localization.get("google_search_label", appLang), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = isSearch,
                    onCheckedChange = { viewModel.setSearchGroundingEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BentoEmerald,
                        checkedTrackColor = BentoEmerald.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }
        }

        // Beautiful Bento Summary Section Header
        BentoSummarySection(viewModel)

        Spacer(modifier = Modifier.height(4.dp))

        // Scrollable Chat area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (isUser) {
                        // User message bubble styled with Bento Indigo gradient / glass
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(BentoIndigo.copy(alpha = 0.75f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = userName.ifBlank { if (appLang == "id") "Anda" else "You" },
                                    fontWeight = FontWeight.Bold,
                                    color = BentoIndigoLight,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = com.example.ui.components.parseMarkdownToAnnotatedString(msg.message),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    } else {
                        // Model response bubble styled with glass background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = aiName.ifBlank { "Gemini AI" },
                                    fontWeight = FontWeight.Bold,
                                    color = BentoCyan,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = com.example.ui.components.parseMarkdownToAnnotatedString(msg.message),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text(Localization.get("thinking", appLang), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Pre-defined quick suggestions pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                Localization.get("quick_sugg_coffee", appLang) to Localization.get("quick_sugg_coffee_full", appLang),
                Localization.get("quick_sugg_thesis", appLang) to Localization.get("quick_sugg_thesis_full", appLang)
            )
            suggestions.forEach { pair ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .clickable { viewModel.setChatInput(pair.second) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = pair.first,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Chat Input box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.setChatInput(it) },
                placeholder = { Text(Localization.get("chat_placeholder", appLang), color = Color.White.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BentoIndigo,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = { viewModel.sendChatMessage() },
                containerColor = BentoIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

// 5. TAB 4: ANALYTICS DASHBOARD (AI INSIGHTS & WHAT-IF PLANNER)
@Composable
fun AnalyticsDashboardScreen(viewModel: FinanceActivityViewModel) {
    val appLang by viewModel.appLanguage.collectAsState()
    val insights by viewModel.aiInsights.collectAsState()
    val briefing by viewModel.morningBriefing.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    // Scenario inputs
    var scenarioType by remember { mutableStateOf("Taking a Loan") }
    var amountStr by remember { mutableStateOf("10000000") }
    var durationMonths by remember { mutableStateOf(12f) }
    var interestRateStr by remember { mutableStateOf("6.0") }
    var selectedGoal by remember { mutableStateOf("Emergency Fund") }
    
    val scenarioTypes = listOf("Taking a Loan", "Increasing Income", "Changing Spending Habits", "Buying Asset")
    val goals = listOf(
        "Emergency Fund" to 15000000.0,
        "Down Payment" to 50000000.0,
        "Retirement" to 100000000.0,
        "Investment Seed" to 10000000.0
    )
    val goalMap = goals.toMap()

    // Calculate current financial standing
    val monthlySalary = transactions.filter { it.category == "Salary" }.sumOf { it.amount }
    val monthlyExpenses = transactions.filter { it.category != "Salary" }.sumOf { it.amount }
    
    // Fallbacks if no data exists yet
    val currentMonthlyIncome = if (monthlySalary > 0.0) monthlySalary else 10000000.0
    val currentMonthlyExpense = if (monthlyExpenses > 0.0) monthlyExpenses else 6000000.0
    val currentMonthlySavings = (currentMonthlyIncome - currentMonthlyExpense).coerceAtLeast(0.0)

    // Compute Scenario Impacts
    val hypAmt = amountStr.toDoubleOrNull() ?: 0.0
    val durationVal = durationMonths.toInt()
    val interestVal = interestRateStr.toDoubleOrNull() ?: 0.0

    var monthlyImpactLabel = ""
    var monthlyImpactValue = 0.0
    var projectedSavingsWithScenario = 0.0
    val projectedSavingsStatusQuo = currentMonthlySavings * durationVal

    when (scenarioType) {
        "Taking a Loan" -> {
            val totalInterest = hypAmt * (interestVal / 100.0) * (durationVal / 12.0)
            val totalRepayment = hypAmt + totalInterest
            val monthlyInstallment = if (durationVal > 0) totalRepayment / durationVal else 0.0
            
            monthlyImpactLabel = if (appLang == "id") "Cicilan Hutang Bulanan" else "Monthly Debt Service"
            monthlyImpactValue = -monthlyInstallment
            projectedSavingsWithScenario = (projectedSavingsStatusQuo - monthlyInstallment * durationVal + hypAmt).coerceAtLeast(0.0)
        }
        "Increasing Income" -> {
            monthlyImpactLabel = if (appLang == "id") "Tambahan Pendapatan Bulanan" else "Additional Monthly Income"
            monthlyImpactValue = hypAmt
            projectedSavingsWithScenario = projectedSavingsStatusQuo + (hypAmt * durationVal)
        }
        "Changing Spending Habits" -> {
            monthlyImpactLabel = if (appLang == "id") "Penghematan Biaya Bulanan" else "Monthly Cost Savings"
            monthlyImpactValue = hypAmt
            projectedSavingsWithScenario = projectedSavingsStatusQuo + (hypAmt * durationVal)
        }
        "Buying Asset" -> {
            monthlyImpactLabel = if (appLang == "id") "Biaya Pembelian Aset Sekaligus" else "One-Off Asset Purchase Cost"
            monthlyImpactValue = -hypAmt
            projectedSavingsWithScenario = (projectedSavingsStatusQuo - hypAmt).coerceAtLeast(0.0)
        }
    }

    val goalAmount = goalMap[selectedGoal] ?: 15000000.0
    val currentMonthlySavingsRate = if (currentMonthlySavings > 0.0) currentMonthlySavings else 1000000.0
    val monthsToGoalStatusQuo = goalAmount / currentMonthlySavingsRate
    
    val scenarioMonthlySavingsRate = (currentMonthlySavingsRate + (if (scenarioType == "Taking a Loan" || scenarioType == "Buying Asset") monthlyImpactValue else monthlyImpactValue)).coerceAtLeast(100000.0)
    val monthsToGoalWithScenario = if (scenarioType == "Taking a Loan" || scenarioType == "Buying Asset") {
        val remainingGoal = (goalAmount - (if (scenarioType == "Taking a Loan") hypAmt else -hypAmt)).coerceAtLeast(0.0)
        remainingGoal / currentMonthlySavingsRate
    } else {
        goalAmount / scenarioMonthlySavingsRate
    }

    // Goal Names localized
    val localizedGoalName = when (selectedGoal) {
        "Emergency Fund" -> if (appLang == "id") "Dana Darurat" else "Emergency Fund"
        "Down Payment" -> if (appLang == "id") "Uang Muka (DP)" else "Down Payment"
        "Retirement" -> if (appLang == "id") "Pensiun / Hari Tua" else "Retirement"
        "Investment Seed" -> if (appLang == "id") "Modal Investasi" else "Investment Seed"
        else -> selectedGoal
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Morning Briefing Section
        item {
            Text(
                Localization.get("morning_briefing_title", appLang),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VividViolet.copy(alpha = 0.2f), VividIndigo.copy(alpha = 0.1f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌅", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("morning_briefing_title", appLang), fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                        }
                        IconButton(onClick = { viewModel.refreshMorningBriefing() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = briefing ?: Localization.get("morning_briefing_desc", appLang),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // What-If Scenario Planning Section
        item {
            Text(
                Localization.get("scenario_planner_title", appLang),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        Localization.get("model_hypo_title", appLang),
                        color = BentoCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 1. Scenario Type Selector
                    Text(Localization.get("select_scenario", appLang), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Button(
                            onClick = { typeExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayedScenarioName = when (scenarioType) {
                                    "Taking a Loan" -> if (appLang == "id") "Mengambil Pinjaman" else "Taking a Loan"
                                    "Increasing Income" -> if (appLang == "id") "Meningkatkan Pendapatan" else "Increasing Income"
                                    "Changing Spending Habits" -> if (appLang == "id") "Mengubah Kebiasaan Belanja" else "Changing Spending Habits"
                                    "Buying Asset" -> if (appLang == "id") "Membeli Aset" else "Buying Asset"
                                    else -> scenarioType
                                }
                                Text(displayedScenarioName, color = Color.White, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            scenarioTypes.forEach { type ->
                                val displayedName = when (type) {
                                    "Taking a Loan" -> if (appLang == "id") "Mengambil Pinjaman" else "Taking a Loan"
                                    "Increasing Income" -> if (appLang == "id") "Meningkatkan Pendapatan" else "Increasing Income"
                                    "Changing Spending Habits" -> if (appLang == "id") "Mengubah Kebiasaan Belanja" else "Changing Spending Habits"
                                    "Buying Asset" -> if (appLang == "id") "Membeli Aset" else "Buying Asset"
                                    else -> type
                                }
                                DropdownMenuItem(
                                    text = { Text(displayedName, color = Color.White) },
                                    onClick = {
                                        scenarioType = type
                                        typeExpanded = false
                                        amountStr = when (type) {
                                            "Taking a Loan" -> "10000000"
                                            "Increasing Income" -> "2000000"
                                            "Changing Spending Habits" -> "1000000"
                                            "Buying Asset" -> "5000000"
                                            else -> "1000000"
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Amount Input
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(Localization.get("scenario_amount", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (scenarioType == "Taking a Loan") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = interestRateStr,
                            onValueChange = { interestRateStr = it },
                            label = { Text(Localization.get("interest_rate", appLang), color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VividViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Duration Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(String.format(Localization.get("timeline_horizon", appLang), durationVal), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    Slider(
                        value = durationMonths,
                        onValueChange = { durationMonths = it },
                        valueRange = 3f..60f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = BentoIndigo,
                            activeTrackColor = BentoIndigo,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Goal Selector
                    Text(Localization.get("evaluate_goal", appLang), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    var goalExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Button(
                            onClick = { goalExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$localizedGoalName (Target: Rp ${String.format("%,.0f", goalAmount)})", color = Color.White, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = goalExpanded,
                            onDismissRequest = { goalExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            goals.forEach { (goalName, goalTarget) ->
                                val nameDisp = when (goalName) {
                                    "Emergency Fund" -> if (appLang == "id") "Dana Darurat" else "Emergency Fund"
                                    "Down Payment" -> if (appLang == "id") "Uang Muka (DP)" else "Down Payment"
                                    "Retirement" -> if (appLang == "id") "Pensiun / Hari Tua" else "Retirement"
                                    "Investment Seed" -> if (appLang == "id") "Modal Investasi" else "Investment Seed"
                                    else -> goalName
                                }
                                DropdownMenuItem(
                                    text = { Text("$nameDisp (Rp ${String.format("%,.0f", goalTarget)})", color = Color.White) },
                                    onClick = {
                                        selectedGoal = goalName
                                        goalExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Results Bento Blocks
                    Text(Localization.get("projected_results", appLang), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Monthly Impact
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(monthlyImpactLabel, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text(Localization.get("monthly_budget_shift", appLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = if (monthlyImpactValue >= 0) "+Rp ${String.format("%,.0f", monthlyImpactValue)}" else "-Rp ${String.format("%,.0f", -monthlyImpactValue)}",
                            color = if (monthlyImpactValue >= 0) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Savings Comparison
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(Localization.get("status_quo_savings", appLang), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Rp ${String.format("%,.0f", projectedSavingsStatusQuo)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(String.format(Localization.get("over_months", appLang), durationVal), color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(VividIndigo.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, VividIndigo.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(Localization.get("scenario_savings", appLang), color = BentoCyan, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Rp ${String.format("%,.0f", projectedSavingsWithScenario)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val diff = projectedSavingsWithScenario - projectedSavingsStatusQuo
                            Text(
                                text = if (diff >= 0) "+Rp ${String.format("%,.0f", diff)}" else "Rp ${String.format("%,.0f", diff)}",
                                color = if (diff >= 0) Color.Green else Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Goal Acceleration
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(VividViolet.copy(alpha = 0.15f), Color.Transparent)
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, VividViolet.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(String.format(Localization.get("goal_impact", appLang), localizedGoalName), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Localization.get("standard_time_to_goal_label", appLang), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text(String.format(Localization.get("months_format", appLang), monthsToGoalStatusQuo), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Localization.get("with_scenario_time_label", appLang), color = BentoCyan, fontSize = 11.sp)
                            Text(String.format(Localization.get("months_format", appLang), monthsToGoalWithScenario), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        
                        val speedup = monthsToGoalStatusQuo - monthsToGoalWithScenario
                        if (speedup > 0.1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Green.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Localization.get("faster_format", appLang), String.format("%.1f", speedup)),
                                    color = Color.Green,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (speedup < -0.1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Localization.get("slower_format", appLang), String.format("%.1f", -speedup)),
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Advanced AI Insights Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Localization.get("behavioral_analytics", appLang),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Button(
                    onClick = { viewModel.generateAiInsights() },
                    colors = ButtonDefaults.buttonColors(containerColor = VividViolet.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Localization.get("analyze_button", appLang), color = Color.White, fontSize = 11.sp)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Localization.get("unified_insights_model", appLang),
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = insights,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

// 6. TAB 5: PROFILE SETTINGS SCREEN
@Composable
fun ProfileSettingsScreen(
    viewModel: FinanceActivityViewModel,
    onLogout: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val slang by viewModel.personaSlang.collectAsState()
    val memories by viewModel.memoryText.collectAsState()

    var editingSlang by remember { mutableStateOf(slang) }
    var editingMemories by remember { mutableStateOf(memories) }

    LaunchedEffect(slang) { editingSlang = slang }
    LaunchedEffect(memories) { editingMemories = memories }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Localization.get("ai_settings_title", appLang),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Language Selector Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Localization.get("language_settings_title", appLang), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(Localization.get("language_settings_subtitle", appLang), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Indonesian button
                        Button(
                            onClick = { viewModel.setAppLanguage("id") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appLang == "id") VividViolet else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appLang == "id") {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(Localization.get("language_id", appLang), color = Color.White, fontSize = 12.sp)
                            }
                        }

                        // English button
                        Button(
                            onClick = { viewModel.setAppLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appLang == "en") VividViolet else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appLang == "en") {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(Localization.get("language_en", appLang), color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // User Name & AI Name Card
        item {
            val userName by viewModel.userName.collectAsState()
            val aiName by viewModel.aiName.collectAsState()
            var editingUserName by remember { mutableStateOf(userName) }
            var editingAiName by remember { mutableStateOf(aiName) }
            LaunchedEffect(userName) { editingUserName = userName }
            LaunchedEffect(aiName) { editingAiName = aiName }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (appLang == "id") "👤 Profil & Identitas AI" else "👤 Profile & AI Identity",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (appLang == "id") {
                            "Atur nama Anda agar AI mengenali pemiliknya, dan beri nama asisten AI Anda."
                        } else {
                            "Set your name so the AI recognizes its owner, and give a name to your AI assistant."
                        },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = editingUserName,
                        onValueChange = { editingUserName = it },
                        label = {
                            Text(
                                text = if (appLang == "id") "Nama Anda (Owner)" else "Your Name (Owner)",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editingAiName,
                        onValueChange = { editingAiName = it },
                        label = {
                            Text(
                                text = if (appLang == "id") "Nama AI Asisten" else "AI Assistant Name",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (editingUserName.isNotBlank()) viewModel.updateUserName(editingUserName)
                            if (editingAiName.isNotBlank()) viewModel.updateAiName(editingAiName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (appLang == "id") "Simpan Identitas" else "Save Identity",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Persona & Slang Control
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Localization.get("slang_style_title", appLang), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(Localization.get("slang_subtitle", appLang), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    OutlinedTextField(
                        value = editingSlang,
                        onValueChange = { editingSlang = it },
                        label = { Text(Localization.get("conversational_slang_label", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.updatePersonaSlang(editingSlang) },
                        colors = ButtonDefaults.buttonColors(containerColor = VividViolet)
                    ) {
                        Text(Localization.get("update_style_btn", appLang), color = Color.White)
                    }
                }
            }
        }

        // Long-term Memories control
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Localization.get("stored_memory", appLang), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(Localization.get("stored_memory_subtitle", appLang), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    OutlinedTextField(
                        value = editingMemories,
                        onValueChange = { editingMemories = it },
                        label = { Text(Localization.get("ai_memory_label", appLang), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updateMemoryText(editingMemories) },
                            colors = ButtonDefaults.buttonColors(containerColor = VividViolet)
                        ) {
                            Text(Localization.get("save_memories_btn", appLang), color = Color.White)
                        }

                        // Weekly compact purge manual button
                        Button(
                            onClick = { viewModel.performWeeklyPurgeAndMemoryCompilation() },
                            colors = ButtonDefaults.buttonColors(containerColor = VividIndigo.copy(alpha = 0.6f))
                        ) {
                            Text(Localization.get("purge_consolidate_btn", appLang), color = Color.White)
                        }
                    }
                }
            }
        }

        // Secure API Key Settings Card
        item {
            val customApiKey by viewModel.customApiKey.collectAsState()
            var editingApiKey by remember { mutableStateOf(customApiKey) }
            LaunchedEffect(customApiKey) { editingApiKey = customApiKey }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (appLang == "id") "🔐 Keamanan & API Key Keluarga" else "🔐 Security & Family API Key",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (appLang == "id") {
                            "Untuk membagikan aplikasi ini kepada 5 anggota keluarga Anda dengan aman:\n" +
                            "1. Anda bisa memasukkan API Key Gemini masing-masing di bawah ini (tersimpan aman di HP).\n" +
                            "2. Agar API Key utama aman dari pencurian, buka Google Cloud Console -> API & Services -> Credentials -> Edit API Key -> Batasi API Key hanya untuk aplikasi Android dengan Package Name 'personal.jarvis.build' dan SHA-1 sidik jari HP Anda."
                        } else {
                            "To securely share this app with your 5 family members:\n" +
                            "1. You can input custom personal Gemini API Keys below (stored securely on device).\n" +
                            "2. To restrict your primary key, visit Google Cloud Console -> API & Services -> Credentials -> Edit API Key -> Restrict API Key to Android Apps with Package Name 'personal.jarvis.build' and your device SHA-1 fingerprint."
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                    )

                    OutlinedTextField(
                        value = editingApiKey,
                        onValueChange = { editingApiKey = it },
                        label = {
                            Text(
                                text = if (appLang == "id") "Kustom Gemini API Key (Opsional)" else "Custom Gemini API Key (Optional)",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.updateCustomApiKey(editingApiKey) },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (appLang == "id") "Simpan API Key Kustom" else "Save Custom API Key",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Production Firebase configuration info
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Localization.get("firebase_sync_title", appLang), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(
                        text = Localization.get("firebase_sync_body", appLang),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Logout
        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = VividRose),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Localization.get("logout_btn", appLang), color = Color.White)
            }
        }
    }
}

// 7. FIXED GLASSMORPHIC BOTTOM NAVIGATION BAR
@Composable
fun GlassmorphicNavigationBar(
    viewModel: FinanceActivityViewModel,
    currentTab: AppTab
) {
    val appLang by viewModel.appLanguage.collectAsState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Underlay background row matching the slate bento theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationBarTabItem(
                    tab = AppTab.FINANCE,
                    icon = Icons.Default.ShoppingCart,
                    label = Localization.get("tab_ledger", appLang),
                    isSelected = currentTab == AppTab.FINANCE,
                    onClick = { viewModel.setTab(AppTab.FINANCE) }
                )

                NavigationBarTabItem(
                    tab = AppTab.CALENDAR,
                    icon = Icons.Default.List,
                    label = Localization.get("tab_planner", appLang),
                    isSelected = currentTab == AppTab.CALENDAR,
                    onClick = { viewModel.setTab(AppTab.CALENDAR) }
                )

                // Placeholder space for the floating center button so the layout is balanced
                Box(modifier = Modifier.size(56.dp))

                NavigationBarTabItem(
                    tab = AppTab.ANALYTICS,
                    icon = Icons.Default.Star,
                    label = Localization.get("tab_insights", appLang),
                    isSelected = currentTab == AppTab.ANALYTICS,
                    onClick = { viewModel.setTab(AppTab.ANALYTICS) }
                )

                NavigationBarTabItem(
                    tab = AppTab.PROFILE,
                    icon = Icons.Default.AccountCircle,
                    label = Localization.get("tab_profile", appLang),
                    isSelected = currentTab == AppTab.PROFILE,
                    onClick = { viewModel.setTab(AppTab.PROFILE) }
                )
            }

            // Floating Center Chat button overlapping the bar
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, ambientColor = BentoIndigo, spotColor = BentoIndigo)
                    .clip(CircleShape)
                    .background(BentoIndigo)
                    .border(4.dp, Color(0xFF020617), CircleShape)
                    .clickable { viewModel.setTab(AppTab.CHAT) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Gemini",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun NavigationBarTabItem(
    tab: AppTab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = BentoCyan
    val inactiveColor = Color.White.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
