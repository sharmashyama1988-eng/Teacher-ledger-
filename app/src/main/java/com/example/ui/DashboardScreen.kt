package com.example.ui

import android.content.Context
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FeeTransaction
import com.example.data.Student
import com.example.data.TeacherNote
import com.example.ui.theme.*

// --- Premium UI & Avatar Initials Styling Helpers ---
fun getInitials(name: String): String {
    val words = name.trim().split("\\s+".toRegex())
    return if (words.size >= 2) {
        (words[0].take(1) + words[1].take(1)).uppercase()
    } else if (name.isNotEmpty()) {
        name.take(2).uppercase()
    } else {
        "ST"
    }
}

fun getInitialsColors(name: String, isDarkMode: Boolean): Pair<Color, Color> {
    val hash = name.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    val pairs = if (isDarkMode) {
        listOf(
            Color(0xFFEADDFF) to Color(0xFF21005D), // Violet Light on Dark text
            Color(0xFFF2B8B5) to Color(0xFF601410), // Red/Pink
            Color(0xFFCCC2DC) to Color(0xFF332D41), // Secondary Sage
            Color(0xFFD2E3FC) to Color(0xFF174EA6), // Blue
            Color(0xFFE6F4EA) to Color(0xFF137333), // Green
            Color(0xFFFEEFC3) to Color(0xFFB06000)  // Yellow/Orange
        )
    } else {
        listOf(
            Color(0xFF21005D) to Color(0xFFEADDFF), // Violet Dark on Light text
            Color(0xFF601410) to Color(0xFFF2B8B5),
            Color(0xFF332D41) to Color(0xFFCCC2DC),
            Color(0xFF174EA6) to Color(0xFFD2E3FC),
            Color(0xFF137333) to Color(0xFFE6F4EA),
            Color(0xFFB06000) to Color(0xFFFEEFC3)
        )
    }
    val idx = if (pairs.isEmpty()) 0 else (hash % pairs.size)
    return pairs[idx]
}

fun Modifier.premiumCardBorder(isDark: Boolean, shape: androidx.compose.ui.graphics.Shape): Modifier {
    val borderColor = if (isDark) Color(0xFF49454F) else Color(0xFFE0DCE6)
    return this.then(
        Modifier.border(width = 1.dp, color = borderColor, shape = shape)
    )
}

// Active Tabs Enum
enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Analytics),
    STUDENTS("Students", Icons.Default.People),
    TOOLS("Tools", Icons.Default.AutoAwesome),
    NOTES("Notes", Icons.Default.NoteAlt),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isLocked by viewModel.isPinLocked.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Observe sync updates and show simple Toast notifications
    LaunchedEffect(syncStatus) {
        syncStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncStatus()
        }
    }

    Scaffold { paddingValues ->
        val bgBrush = if (isDarkMode) {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF262135),
                    Color(0xFF1C1B1F),
                    Color(0xFF131115)
                )
            )
        } else {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF3EDF7),
                    Color(0xFFFEF7FF),
                    Color(0xFFF1EEF5)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(bgBrush)
        ) {
            if (isLocked) {
                // PIN Lock Screen overlay
                PinLockScreen(
                    onVerify = { pin ->
                        val success = viewModel.verifyPinAndUnlock(pin)
                        if (!success) {
                            Toast.makeText(context, "Incorrect PIN Code!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                // Main Application Layout
                MainContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PinLockScreen(onVerify: (String) -> Unit) {
    var inputPin by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Teacher Ledger Locked",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Enter your 4-digit secure PIN to access",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Circular entry indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(4) { idx ->
                val filled = idx < inputPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                        )
                )
            }
        }

        // Numerical keypad layout
        val keypad = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "Unlock")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            for (row in keypad) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (key in row) {
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                                    "Unlock" -> {
                                        if (inputPin.length == 4) {
                                            onVerify(inputPin)
                                            inputPin = ""
                                        }
                                    }
                                    else -> {
                                        if (inputPin.length < 4) {
                                            inputPin += key
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "Unlock") MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (key == "Unlock") MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("keypad_$key")
                        ) {
                            Text(
                                text = key,
                                fontSize = if (key == "Unlock") 12.sp else 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val context = LocalContext.current

    // Bottom Navigation Bar
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> direction * width / 4 } + 
                     fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                    (slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) { width -> direction * -width / 4 } + 
                     fadeOut(animationSpec = tween(durationMillis = 140)))
                },
                label = "TabTransition"
            ) { targetState ->
                when (targetState) {
                    AppTab.DASHBOARD -> DashboardTab(viewModel = viewModel)
                    AppTab.STUDENTS -> StudentsDirectoryTab(viewModel = viewModel)
                    AppTab.TOOLS -> TeacherToolsTab(viewModel = viewModel)
                    AppTab.NOTES -> TeacherNotesTab(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsPropertyTab(viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// TAB 1: DASHBOARD
// ==========================================
@Composable
fun DashboardTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sums by viewModel.portfolioSums.collectAsState()
    val rawEarnings by viewModel.monthlyEarnings.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val pendingCount = remember(students) { students.count { !it.status.equals("PAID", ignoreCase = true) } }

    var showCollectReceiptDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Headline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EduTrack Pro",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Professional Educator Workspace",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.triggerPrintPdf(context) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDarkMode) Color(0xFF353439) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isDarkMode) Color(0xFFD0BCFF) else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .premiumCardBorder(isDarkMode, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export Report as PDF", modifier = Modifier.size(20.dp))
                }
            }
        }

        // Summary Analytics section: 2 Column Quick Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Earning Card (Collected)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .premiumCardBorder(isDarkMode, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF353439) else Color(0xFFF3EDF7)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EARNINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color(0xFF938F99) else Color(0xFF625B71),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${sums.totalCollectedFees}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Collected Fees",
                                color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF137333),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Pending Dues Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .premiumCardBorder(isDarkMode, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF353439) else Color(0xFFFBEBEB)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PENDING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color(0xFF938F99) else Color(0xFF625B71),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${sums.totalPendingDues}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$pendingCount Students Dues",
                                color = if (isDarkMode) Color(0xFFF2B8B5) else Color(0xFFB3261E),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Target Collection Progress Card
        item {
            val total = if (sums.totalRequiredFees > 0) sums.totalRequiredFees else 1.0
            val ratio = (sums.totalCollectedFees / total).toFloat().coerceIn(0f, 1f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCardBorder(isDarkMode, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF2B2930) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PAYMENT PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${(ratio * 100).toInt()}% Target",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isDarkMode) Color(0xFF49454F) else Color(0xFFEADDFF).copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Prescribed: ₹${sums.totalRequiredFees}   •   Remaining Outstandings: ₹${sums.totalPendingDues}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Mini Quick Buttons Column (Allows Excel and Dues extraction)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.intentShareCsv(context, "STUDENTS") },
                    modifier = Modifier
                        .weight(1f)
                        .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color(0xFF2B2930) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Dataset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Excel CSV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { showCollectReceiptDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Collect Fees", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Monthly Earnings Summary List Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCardBorder(isDarkMode, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF2B2930) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MONTHLY EARNINGS LEAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    if (rawEarnings.isEmpty()) {
                        Text(
                            text = "No collection payments recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        var idx = 0
                        rawEarnings.forEach { (month, sum) ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = if (isDarkMode) Color(0xFF49454F).copy(alpha = 0.5f) else Color(0xFFEADDFF).copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(month, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Text("₹$sum", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            idx++
                        }
                    }
                }
            }
        }

        // Recent Transaction History Ledger
        item {
            Text(
                text = "Recent Payments Checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Collect a fee payment using the quick action button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = transactions.take(5),
                key = { tx -> tx.id }
            ) { tx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF2B2930) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Initials for payment sender
                            val (bgCol, txtCol) = getInitialsColors(tx.studentName, isDarkMode)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgCol),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getInitials(tx.studentName),
                                    color = txtCol,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(tx.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val modeIcon = when (tx.paymentMode.uppercase()) {
                                        "UPI" -> Icons.Default.QrCode
                                        "BANK" -> Icons.Default.AccountBalance
                                        else -> Icons.Default.Payments
                                    }
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${tx.monthYear} • ${tx.paymentMode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+₹${tx.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                            
                            IconButton(onClick = { viewModel.removePaymentTransaction(tx) }) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete fee transaction log", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialog to Collect payment ---
    if (showCollectReceiptDialog) {
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }
        var amountStr by remember { mutableStateOf("") }
        var selectedMode by remember { mutableStateOf("Cash") }
        var remarksStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCollectReceiptDialog = false },
            title = { Text("Log Fee Receipt") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Custom select drop down for Student picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedStudent?.name ?: "Select Class Student ▼")
                        }
                        
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            if (students.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Students Available. Create one first!") },
                                    onClick = { dropdownExpanded = false }
                                )
                            } else {
                                students.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (Pending: ₹${s.balancePending})") },
                                        onClick = {
                                            selectedStudent = s
                                            amountStr = s.balancePending.toString() // Autofill due values
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount Collected (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment modes buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Cash", "UPI", "Bank").forEach { mode ->
                            val active = selectedMode == mode
                            Button(
                                onClick = { selectedMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = remarksStr,
                        onValueChange = { remarksStr = it },
                        label = { Text("Receipt Comments/Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        val student = selectedStudent
                        if (student != null && amount != null && amount > 0.0) {
                            viewModel.addPaymentTransaction(
                                studentId = student.id,
                                studentName = student.name,
                                amount = amount,
                                mode = selectedMode,
                                remarks = remarksStr
                            )
                            Toast.makeText(context, "Receipt Logged Successfully!", Toast.LENGTH_SHORT).show()
                            showCollectReceiptDialog = false
                        } else {
                            Toast.makeText(context, "Fill all details correctly!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCollectReceiptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 2: STUDENTS DIRECTORY
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsDirectoryTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val students by viewModel.filteredStudents.collectAsState()
    val searchQuery by viewModel.studentSearchQuery.collectAsState()
    val statusFilter by viewModel.selectedStatusFilter.collectAsState()

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var viewingStudent by remember { mutableStateOf<Student?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Class directory search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name, roll number...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips list
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("ALL", "PAID", "PENDING").forEach { filter ->
                val active = statusFilter == filter
                FilterChip(
                    selected = active,
                    onClick = { viewModel.setStatusFilter(filter) },
                    label = { Text(filter, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (students.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No students found matching current filters.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = students,
                        key = { student -> student.id }
                    ) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewingStudent = student },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF2B2930) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Initials sender badge
                                val (bgCol, txtCol) = getInitialsColors(student.name, isDarkMode)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgCol),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getInitials(student.name),
                                        color = txtCol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        student.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (student.rollNum.isNotEmpty()) "Roll Num: ${student.rollNum}" else "No Roll ID",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                // Custom due status badge
                                val pendingAmount = student.balancePending
                                val isPaid = student.status == "PAID"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isPaid) PaidGreenLight else PendingRedLight
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isPaid) "Paid" else "Due ₹$pendingAmount",
                                        color = if (isPaid) PaidGreenTextLight else PendingRedTextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Simple FAB to Add Student
            FloatingActionButton(
                onClick = { showAddStudentDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_student_fab")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add New student profile record")
            }
        }
    }

    // --- Action Sheet/Dialog to view Student and log fees/sync ---
    viewingStudent?.let { student ->
        var showSingleStudentFeeCollectDialog by remember { mutableStateOf(false) }
        var showUpiQrDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewingStudent = null },
            title = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(student.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (student.rollNum.isNotEmpty()) "Roll: ${student.rollNum}" else "No assigned roll ID",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    // Status Badge
                    val isPaid = student.status == "PAID"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPaid) PaidGreenLight else PendingRedLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            student.status,
                            color = if (isPaid) PaidGreenTextLight else PendingRedTextLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider()
                    Text("Total Fees Prescribed: ₹${student.totalFee}", fontSize = 13.sp)
                    Text("Amount Paid: ₹${student.paidAmount}", fontSize = 13.sp)
                    Text("Current Back Dues: ₹${student.balancePending}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (student.balancePending > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    
                    if (student.phone.isNotEmpty()) {
                        Text("Phone Number: ${student.phone}", fontSize = 13.sp)
                    }
                    if (student.remarks.isNotEmpty()) {
                        Text("Special Remarks: ${student.remarks}", fontSize = 13.sp)
                    }

                    if (student.lastPaymentDate > 0) {
                        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(student.lastPaymentDate))
                        Text("Last Payment Received: $formattedDate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    // Smart Payment & Reminder Suite
                    if (student.balancePending > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF2C2A33) else Color(0xFFF6F3F8)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Smart Payment & Reminders",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Send direct alerts with auto-filled payment details or generate on-screen UPI QR code for parent scanning.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Button 1: QR code popup
                                    OutlinedButton(
                                        onClick = { showUpiQrDialog = true },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Show QR", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Button 2: WhatsApp Hinglish reminder
                                    Button(
                                        onClick = {
                                            val academyText = viewModel.settings.academyName
                                            val upiText = viewModel.settings.upiId
                                            val hinglishMsg = "नमस्ते! ${if (academyText.isNotEmpty()) academyText else "ट्यूशन क्लासेज"} से ${student.name} के बकाया ट्यूशन फीस (₹${student.balancePending.toInt()}) का भुगतान याद दिलाने के लिए यह संदेश है।\n" +
                                                    "कुल फीस: ₹${student.totalFee.toInt()}\n" +
                                                    "जमा फीस: ₹${student.paidAmount.toInt()}\n" +
                                                    "बकाया राशि: ₹${student.balancePending.toInt()}\n\n" +
                                                    "कृपया बकाया फीस का भुगतान इस UPI आईडी पर करें: ${if (upiText.isNotEmpty()) upiText else "नोटीफाइड UPI"}\n" +
                                                    (if (upiText.isNotEmpty()) "भुगतान डायरेक्ट लिंक: upi://pay?pa=$upiText&pn=${Uri.encode(academyText)}&am=${student.balancePending}&cu=INR\n" else "") +
                                                    "धन्यवाद!"
                                            try {
                                                val cleanPhone = student.phone.replace("+", "").replace("-", "").replace(" ", "").trim()
                                                val formattedNumber = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(hinglishMsg)}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, hinglishMsg)
                                                        type = "text/plain"
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    val shareIntent = Intent.createChooser(sendIntent, "Share Hinglish Reminder").apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(shareIntent)
                                                } catch (ex: Exception) {
                                                    Toast.makeText(context, "Cannot open Share Sheet: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PaidGreenTextLight),
                                        modifier = Modifier.weight(1.2f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Hinglish WA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Button 3: WhatsApp English reminder
                                    Button(
                                        onClick = {
                                            val academyText = viewModel.settings.academyName
                                            val upiText = viewModel.settings.upiId
                                            val englishMsg = "Hello! This is a reminder from ${if (academyText.isNotEmpty()) academyText else "Tuition Classes"} regarding the outstanding tuition fee for student ${student.name}.\n" +
                                                    "Total Fee: ₹${student.totalFee.toInt()}\n" +
                                                    "Amount Paid: ₹${student.paidAmount.toInt()}\n" +
                                                    "Pending Dues: ₹${student.balancePending.toInt()}\n\n" +
                                                    "Kindly pay the pending dues via UPI directly to ID: ${if (upiText.isNotEmpty()) upiText else "provided UPI ID"}\n" +
                                                    (if (upiText.isNotEmpty()) "Direct Pay link: upi://pay?pa=$upiText&pn=${Uri.encode(academyText)}&am=${student.balancePending}&cu=INR\n" else "") +
                                                    "Thank you!"
                                            try {
                                                val cleanPhone = student.phone.replace("+", "").replace("-", "").replace(" ", "").trim()
                                                val formattedNumber = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(englishMsg)}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, englishMsg)
                                                        type = "text/plain"
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    val shareIntent = Intent.createChooser(sendIntent, "Share English Reminder").apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(shareIntent)
                                                } catch (ex: Exception) {
                                                    Toast.makeText(context, "Cannot open Share Sheet: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1.2f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("English WA", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick sync Button to post ledger fields to google forms
                        OutlinedButton(
                            onClick = { 
                                if (viewModel.settings.isGoogleFormSyncEnabled) {
                                    viewModel.syncSingleStudentDirectly(student)
                                } else {
                                    Toast.makeText(context, "Enable & config Google Form Sync in Settings!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Sheet Form Sync", fontSize = 9.sp)
                        }

                        Button(
                            onClick = { showSingleStudentFeeCollectDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Fee Payment", fontSize = 9.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStudent(student)
                        Toast.makeText(context, "Student records removed successfully.", Toast.LENGTH_SHORT).show()
                        viewingStudent = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Student")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingStudent = null }) {
                    Text("Dismiss")
                }
            }
        )

        // Nested Dialog for QR Code Presentation
        if (showUpiQrDialog) {
            val academyText = viewModel.settings.academyName
            val upiText = viewModel.settings.upiId
            
            AlertDialog(
                onDismissRequest = { showUpiQrDialog = false },
                title = { Text("Scan to Pay: ${student.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (upiText.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "UPI ID is not configured! Please configure your UPI VPA ID in the Settings tab first to generate scan QR codes.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        } else {
                            val upiUrl = "upi://pay?pa=$upiText&pn=${Uri.encode(academyText)}&am=${student.balancePending}&cu=INR&tn=${Uri.encode("TuitionFee_${student.name}")}"
                            val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=${Uri.encode(upiUrl)}"
                            
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = qrCodeUrl,
                                    contentDescription = "Scan to Pay QR Code",
                                    modifier = Modifier.fillMaxSize(),
                                    alignment = Alignment.Center
                                )
                            }
                            
                            Text(
                                "Outstanding Dues: ₹${student.balancePending.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                "UPI ID: $upiText\nAcademy: ${academyText.ifEmpty { "Tuition Classes" }}",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            // Pay on device
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No local UPI apps found to process deep link!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pay on Device (Open UPI apps)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showUpiQrDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Nested Dialog for collecting specific student's payment receipts
        if (showSingleStudentFeeCollectDialog) {
            var collectAmtStr by remember { mutableStateOf(student.balancePending.toString()) }
            var paymentModeSelect by remember { mutableStateOf("Cash") }
            var commentsStr by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showSingleStudentFeeCollectDialog = false },
                title = { Text("Receipt: ${student.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = collectAmtStr,
                            onValueChange = { collectAmtStr = it },
                            label = { Text("Amount Logged (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Cash", "UPI", "Bank").forEach { mode ->
                                val active = paymentModeSelect == mode
                                Button(
                                    onClick = { paymentModeSelect = mode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = commentsStr,
                            onValueChange = { commentsStr = it },
                            label = { Text("Receipt Comments/Remarks") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = collectAmtStr.toDoubleOrNull()
                            if (amount != null && amount > 0.0) {
                                viewModel.addPaymentTransaction(
                                    studentId = student.id,
                                    studentName = student.name,
                                    amount = amount,
                                    mode = paymentModeSelect,
                                    remarks = commentsStr
                                )
                                Toast.makeText(context, "Collected successfully!", Toast.LENGTH_SHORT).show()
                                showSingleStudentFeeCollectDialog = false
                                viewingStudent = null
                            } else {
                                Toast.makeText(context, "Fill the correct amount!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Log Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSingleStudentFeeCollectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // --- Dialog to Add Student ---
    if (showAddStudentDialog) {
        var nameStr by remember { mutableStateOf("") }
        var rollStr by remember { mutableStateOf("") }
        var phoneStr by remember { mutableStateOf("") }
        var remarksStr by remember { mutableStateOf("") }
        var totalFeeStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Register Class Student") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = { Text("Student Full Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("add_student_name_input")
                    )

                    OutlinedTextField(
                        value = rollStr,
                        onValueChange = { rollStr = it },
                        label = { Text("Class Roll Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneStr,
                        onValueChange = { phoneStr = it },
                        label = { Text("Guardian Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = totalFeeStr,
                        onValueChange = { totalFeeStr = it },
                        label = { Text("Total Prescribed Fee (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = remarksStr,
                        onValueChange = { remarksStr = it },
                        label = { Text("Student Details / Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fee = totalFeeStr.toDoubleOrNull()
                        if (nameStr.isNotEmpty() && fee != null) {
                            viewModel.addStudent(
                                name = nameStr,
                                rollNum = rollStr,
                                phone = phoneStr,
                                remarks = remarksStr,
                                totalFee = fee
                            )
                            Toast.makeText(context, "Student Registered Locally!", Toast.LENGTH_SHORT).show()
                            showAddStudentDialog = false
                        } else {
                            Toast.makeText(context, "Name and Fee are required fields!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("add_student_save_button")
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 3: TEACHER NOTES (Remarks/Homework Log)
// ==========================================
@Composable
fun TeacherNotesTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val notes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.noteSearchQuery.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var viewingNote by remember { mutableStateOf<TeacherNote?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setNoteSearchQuery(it) },
            placeholder = { Text("Search comments or student remarks...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No teacher remarks or notes logged.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = notes,
                        key = { note -> note.id }
                    ) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewingNote = note },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF2B2930) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = note.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                
                                if (note.linkedStudentName != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isDarkMode) Color(0xFF353439) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            )
                                            .premiumCardBorder(isDarkMode, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Linked Student: ${note.linkedStudentName}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color(0xFFD0BCFF) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.NoteAdd, contentDescription = "Add General Lecture remark")
            }
        }
    }

    // --- Dialog to edit/delete Note ---
    viewingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { viewingNote = null },
            title = { Text(note.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(note.content, fontSize = 14.sp)
                    if (note.linkedStudentName != null) {
                        Text("Linked to: ${note.linkedStudentName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    val dateFormatted = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.createdAt))
                    Text("Logged date: $dateFormatted", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTeacherNote(note)
                        Toast.makeText(context, "Remark discarded.", Toast.LENGTH_SHORT).show()
                        viewingNote = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingNote = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // --- Dialog to Add Note ---
    if (showAddNoteDialog) {
        var titleStr by remember { mutableStateOf("") }
        var contentStr by remember { mutableStateOf("") }
        var dropdownExp by remember { mutableStateOf(false) }
        var selectedStudentLink by remember { mutableStateOf<Student?>(null) }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Log Remark / Entry") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = titleStr,
                        onValueChange = { titleStr = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contentStr,
                        onValueChange = { contentStr = it },
                        label = { Text("Content Notes...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional link student
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExp = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedStudentLink?.name ?: "Optional: Link to Student ▼")
                        }
                        
                        DropdownMenu(
                            expanded = dropdownExp,
                            onDismissRequest = { dropdownExp = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedStudentLink = null
                                    dropdownExp = false
                                }
                            )
                            students.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        selectedStudentLink = s
                                        dropdownExp = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleStr.isNotEmpty()) {
                            viewModel.addTeacherNote(titleStr, contentStr, selectedStudentLink)
                            showAddNoteDialog = false
                        } else {
                            Toast.makeText(context, "Title is mandatory!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 4: SETTINGS, BACKUP, GOOGLE FORMS MAP
// ==========================================
@Composable
fun SettingsPropertyTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkMode.collectAsState()

    // Config form settings
    var syncEnabled by remember { mutableStateOf(viewModel.settings.isGoogleFormSyncEnabled) }
    var formUrl by remember { mutableStateOf(viewModel.settings.googleFormUrl) }
    var formName by remember { mutableStateOf(viewModel.settings.formEntryName) }
    var formRoll by remember { mutableStateOf(viewModel.settings.formEntryRoll) }
    var formTotalFee by remember { mutableStateOf(viewModel.settings.formEntryTotalFee) }
    var formPaid by remember { mutableStateOf(viewModel.settings.formEntryPaid) }
    var formBalance by remember { mutableStateOf(viewModel.settings.formEntryBalance) }

    // UPI academy configuration state
    var upiId by remember { mutableStateOf(viewModel.settings.upiId) }
    var academyName by remember { mutableStateOf(viewModel.settings.academyName) }

    // Backup import text box
    var showImportBackupDialog by remember { mutableStateOf(false) }

    // Local Security PIN Setup Dialog
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var isPinActive by remember { mutableStateOf(viewModel.settings.isPinLockEnabled) }

    // Firebase Auth Simulator Dialog
    var showAuthDialog by remember { mutableStateOf(false) }
    var loggedInEmail by remember { mutableStateOf(viewModel.settings.firebaseUserEmail) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Preferences & Admin Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Section: Academy & UPI Payment configuration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCardBorder(isDarkTheme, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF201E24) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Payment Config",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Academy & UPI Payment Config",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Set up your academy name and UPI ID. This allows parents to pay you directly via scanning dynamic QR codes or opening automated WhatsApp links.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    OutlinedTextField(
                        value = academyName,
                        onValueChange = {
                            academyName = it
                            viewModel.settings.academyName = it
                        },
                        label = { Text("Academy / Specialist Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Sharma Tuition Classes", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = {
                            upiId = it
                            viewModel.settings.upiId = it
                        },
                        label = { Text("Your UPI ID for Receipts (VPA)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. sharma@upi, name@ybl etc.", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Section 1: Security and Theme
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Local Security & Themes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme Display Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Enables dark, eye-friendly contrasts for low light usage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Local Safe Pin-Lock Encryption", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isPinActive) "Encrypted and secured lock is ACTIVE" else "Lock disabled - casual mode active",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isPinActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showPinSetupDialog = true
                                } else {
                                    viewModel.disablePinLock()
                                    isPinActive = false
                                    Toast.makeText(context, "Lock security switched OFF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section 2: Real-time Firebase Authentication Mock Module
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cloud Sync (Firebase Auth Engine)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Simulate live device-to-cloud profile auth syncing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    if (loggedInEmail.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("No profile authenticated yet.", fontSize = 13.sp)
                            Button(
                                onClick = { showAuthDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign In with Firebase", fontSize = 11.sp)
                            }
                        }
                    } else {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("Authenticated User Email:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(loggedInEmail, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.settings.logoutFirebase()
                                        loggedInEmail = ""
                                        Toast.makeText(context, "Logged out from Cloud", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Disconnect", fontSize = 11.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = PaidGreenTextLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connected to Realtime Storage Client Sync", fontSize = 11.sp, color = PaidGreenTextLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Google Forms Save Mappings Configurations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Google Form Save Integration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = {
                                syncEnabled = it
                                viewModel.settings.isGoogleFormSyncEnabled = it
                            }
                        )
                    }
                    Text("Auto-publish student transaction updates to standard forms.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    HorizontalDivider()

                    if (syncEnabled) {
                        OutlinedTextField(
                            value = formUrl,
                            onValueChange = {
                                formUrl = it
                                viewModel.settings.googleFormUrl = it
                            },
                            label = { Text("Google Form POST Endpoint") },
                            placeholder = { Text("https://docs.google.com/forms/d/e/.../formResponse") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Field Mapping parameters (Check Google Form structure):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = formName,
                                onValueChange = { formName = it; viewModel.settings.formEntryName = it },
                                label = { Text("Name Parameter Key") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formRoll,
                                onValueChange = { formRoll = it; viewModel.settings.formEntryRoll = it },
                                label = { Text("Roll Parameter Key") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = formTotalFee,
                                onValueChange = { formTotalFee = it; viewModel.settings.formEntryTotalFee = it },
                                label = { Text("Fee Key") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formPaid,
                                onValueChange = { formPaid = it; viewModel.settings.formEntryPaid = it },
                                label = { Text("Paid Key") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formBalance,
                                onValueChange = { formBalance = it; viewModel.settings.formEntryBalance = it },
                                label = { Text("Balance Key") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Manual Data Backup and Restore Operations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Encrypted Backups & Offline Portability", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Maintain a complete copy of student profiles, remarks and earnings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    HorizontalDivider()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                viewModel.performBackupShare(context)
                                Toast.makeText(context, "Backup JSON file compiled!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export JSON Backup", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { showImportBackupDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Database", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // --- Firebase Auth simulated Sign in Dialog ---
    if (showAuthDialog) {
        var emailStr by remember { mutableStateOf("") }
        var passStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Firebase Client Auth") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connect your email securely to verify syncing across other connected classroom tablets.", fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = emailStr,
                        onValueChange = { emailStr = it },
                        label = { Text("Educator Email Addr") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passStr,
                        onValueChange = { passStr = it },
                        label = { Text("Account Passcode / Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailStr.contains("@") && passStr.length >= 6) {
                            viewModel.settings.firebaseUserEmail = emailStr
                            viewModel.settings.isFirebaseSyncActive = true
                            loggedInEmail = emailStr
                            Toast.makeText(context, "Securely authenticated. Sync is LIVE", Toast.LENGTH_SHORT).show()
                            showAuthDialog = false
                        } else {
                            Toast.makeText(context, "Incorrect email format or password under 6 chars!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Verify Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Safe PIN Code Lock Generator Dialog ---
    if (showPinSetupDialog) {
        var pinVal by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false; isPinActive = false },
            title = { Text("Configure Local Device lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a 4-digit code combination to secure balance details offline from pupil inspection.", fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = pinVal,
                        onValueChange = { if (it.length <= 4) pinVal = it },
                        label = { Text("Set 4-Digit Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinVal.length == 4 && pinVal.toIntOrNull() != null) {
                            viewModel.setupNewPin(pinVal)
                            isPinActive = true
                            Toast.makeText(context, "Local PIN code enabled successfully!", Toast.LENGTH_SHORT).show()
                            showPinSetupDialog = false
                        } else {
                            Toast.makeText(context, "Provide exactly a 4-digit numeric code!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Locked PIN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPinSetupDialog = false
                        isPinActive = false 
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- JSON Text Backup Import Dialog ---
    if (showImportBackupDialog) {
        var backupPasteStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportBackupDialog = false },
            title = { Text("Paste DB Backup JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Paste the contents of your exported backup file below to restore student catalogs instantly.", fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = backupPasteStr,
                        onValueChange = { backupPasteStr = it },
                        maxLines = 10,
                        placeholder = { Text("{\n  \"version\": 1,\n  \"students\": [...]\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.performRestore(backupPasteStr)
                        if (success) {
                            Toast.makeText(context, "Local student databases restored successfully!", Toast.LENGTH_SHORT).show()
                            showImportBackupDialog = false
                        } else {
                            Toast.makeText(context, "Invalid JSON structure detected!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Trigger Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 5: ADVANCED TEACHER TOOLS WORKSPACE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherToolsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val students by viewModel.allStudents.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Tools Hub",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Educator Toolkit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generative AI Co-pilot assistant for teachers",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Content viewport
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GeminiCopilotView(viewModel, students, isDarkMode)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiCopilotView(viewModel: MainViewModel, students: List<Student>, isDarkMode: Boolean) {
    val context = LocalContext.current
    var subjectTopic by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Lesson Planner") }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var showStudentDropdown by remember { mutableStateOf(false) }

    val aiResult by viewModel.aiGenerationResult.collectAsState()
    val aiLoading by viewModel.aiGenerationLoading.collectAsState()

    val roles = listOf("Lesson Planner", "Quiz Questions", "Reminder SMS", "Progress Remarks")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
    ) {
        // Form parameters card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF201E24) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AI Generation Parameters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Role selection chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Task Category", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            roles.forEach { role ->
                                FilterChip(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role },
                                    label = { Text(role, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Student dropdown button selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Personalize Target Student", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkMode) Color(0xFF2C2A33) else Color(0xFFF1EEF4))
                                .clickable { showStudentDropdown = true }
                                .premiumCardBorder(isDarkMode, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Target icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedStudent?.let { "${it.name} (${it.rollNum})" } ?: "Whole Class (Generic Plan)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Drop down trigger",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Text Field for subject topic details
                    OutlinedTextField(
                        value = subjectTopic,
                        onValueChange = { subjectTopic = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Topic Focus Area or Module title", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Gravity basics, Quadratic Equations, photosynthesis", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Generation button
                    Button(
                        onClick = {
                            if (subjectTopic.trim().isEmpty() && selectedRole != "Reminder SMS") {
                                Toast.makeText(context, "Please write context details / topic focus first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val currentStudent = selectedStudent
                            val queryText = java.lang.StringBuilder()
                            when (selectedRole) {
                                "Lesson Planner" -> {
                                    queryText.append("You are an expert curriculum planner. Create a high-fidelity lesson plan for the subject topic: '$subjectTopic'.")
                                    if (currentStudent != null) {
                                        queryText.append(" Make accommodations or personalization strategies targeting student '${currentStudent.name}'.")
                                    }
                                    queryText.append("\nOutput in structured sections: Summary, Core Concepts, Exercises, Quiz targets.")
                                }
                                "Quiz Questions" -> {
                                    queryText.append("Generate a premium 5-question multiple choice questionnaire on topic: '$subjectTopic'.")
                                    if (currentStudent != null) {
                                        queryText.append(" Calibrate difficulty index customized to the learning levels of student '${currentStudent.name}'.")
                                    }
                                    queryText.append("\nState correct option and a single line explanation.")
                                }
                                "Reminder SMS" -> {
                                    queryText.append("Draft a polite, respectful monthly fee/due reminder message template to be sent to parents")
                                    if (currentStudent != null) {
                                        queryText.append(" of student '${currentStudent.name}'.")
                                        val pendingDues = currentStudent.totalFee - currentStudent.paidAmount
                                        if (pendingDues > 0) {
                                            queryText.append(" Keep them informed that their current outstanding balance is ₹${pendingDues.toInt()}.")
                                        } else {
                                            queryText.append(" Acknowledge that they have completed all dues for the cycle and thank them.")
                                        }
                                    } else {
                                        queryText.append(" of students. Keep placeholder slots for details.")
                                    }
                                    queryText.append("\nFormat as SMS/WhatsApp ready clean text.")
                                }
                                else -> { // Progress Remarks
                                    queryText.append("Create a professional student performance feedback remarks template focusing on: '$subjectTopic'.")
                                    if (currentStudent != null) {
                                        queryText.append(" Personalized for: '${currentStudent.name}' (Roll Number ${currentStudent.rollNum}).")
                                    }
                                    queryText.append("\nInclude 2 commendations and 1 actionable area of tutoring improvement.")
                                }
                            }
                            viewModel.generateWithGemini(queryText.toString())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft with Gemini AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Output Result card
        if (aiResult != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumCardBorder(isDarkMode, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1C20) else Color(0xFFFBF8FD)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Draft Preview Workspace",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (aiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { viewModel.clearAiResult() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss display card",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Scrollable response viewport Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 300.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkMode) Color(0xFF26242B) else Color(0xFFECE7EE))
                                .padding(12.dp)
                        ) {
                            val resultText = aiResult ?: ""
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = resultText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        // Action control tray
                        if (!aiLoading && aiResult != "Thinking...") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save inside note list
                                Button(
                                    onClick = {
                                        val payload = aiResult ?: ""
                                        if (payload.isNotEmpty()) {
                                            val reportTitle = when (selectedRole) {
                                                "Lesson Planner" -> "Lesson Plan: $subjectTopic"
                                                "Quiz Questions" -> "AI MCQ: $subjectTopic"
                                                "Reminder SMS" -> "Payment Alert: ${selectedStudent?.name ?: "All"}"
                                                else -> "AI Remarks: ${selectedStudent?.name ?: "All"}"
                                            }
                                            viewModel.addTeacherNote(
                                                title = reportTitle,
                                                content = payload,
                                                linkedStudent = selectedStudent
                                            )
                                            Toast.makeText(context, "Saved successfully to saved notes!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save to Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Clipboard sync icon
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Copilot Draft", aiResult)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied content to Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Text", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Student selection Dropdown Dialog
    if (showStudentDropdown) {
        AlertDialog(
            onDismissRequest = { showStudentDropdown = false },
            title = { Text("Select Target Student", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    item {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedStudent = null
                                showStudentDropdown = false
                            }
                        ) {
                            Text("Whole Class (Generic Plan)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                    items(
                        items = students,
                        key = { student -> student.id }
                    ) { student ->
                        TextButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            onClick = {
                                selectedStudent = student
                                showStudentDropdown = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = student.name,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (student.balancePending > 0) {
                                    Text(
                                        text = "Dues: ₹${student.balancePending.toInt()}",
                                        color = Color(0xFFD32F2F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStudentDropdown = false }) {
                    Text("Close")
                }
            }
        )
    }
}

