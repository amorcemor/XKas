    package com.mibi.xkas.ui.addedit

    import android.widget.Toast
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.defaultMinSize
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.offset
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardActions
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.CalendarToday
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults
    import androidx.compose.material3.Checkbox
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.DatePicker
    import androidx.compose.material3.DatePickerDialog
    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Tab
    import androidx.compose.material3.TabRow
    import androidx.compose.material3.TabRowDefaults
    import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    import androidx.compose.material3.rememberDatePickerState
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.collectAsState
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.focus.FocusDirection
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalFocusManager
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.input.ImeAction
    import androidx.compose.ui.text.input.KeyboardCapitalization
    import androidx.compose.ui.text.input.KeyboardType
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.unit.dp
    import androidx.hilt.navigation.compose.hiltViewModel
    import com.mibi.xkas.ui.theme.MyButtonBlackBlue
    import com.mibi.xkas.ui.theme.MyButtonOrange
    import com.mibi.xkas.ui.theme.MyWhiteContent
    import com.mibi.xkas.ui.theme.XKasTheme
    import com.mibi.xkas.ui.theme.mainContentCornerRadius
    import java.text.SimpleDateFormat
    import java.util.Calendar
    import java.util.Locale
    import java.util.TimeZone
    import com.mibi.xkas.ui.addedit.NumberDecimalVisualTransformation


    enum class TransactionType {
        PEMASUKAN, PENGELUARAN
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddEditTransactionScreen( // Ganti nama Composable jika nama file berubah
        onNavigateUp: () -> Unit,
        onTransactionSavedSuccessfully: () -> Unit,
        viewModel: AddEditTransactionViewModel = hiltViewModel(),
        modifier: Modifier = Modifier// Gunakan AddTransactionViewModel
    ) {
        // selectedTransactionType sekarang diambil dari ViewModel
        val selectedTransactionType by viewModel.selectedUITransactionType.collectAsState()

        val description by viewModel.description.collectAsState()
        val amount by viewModel.amount.collectAsState()
        val sellingPrice by viewModel.sellingPrice.collectAsState()
        val transactionDateString by viewModel.transactionDate.collectAsState()

        val isDebt by viewModel.isDebt.collectAsState()
        val debtorName by viewModel.debtorName.collectAsState()
        val debtorPhone by viewModel.debtorPhone.collectAsState()

        var showDatePickerDialog by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        val context = LocalContext.current

        val saveState by viewModel.saveTransactionUiState.collectAsState()
        val isEditMode = viewModel.isEditMode // Dapatkan dari ViewModel

        val numberDecimalVisualTransformation = remember { NumberDecimalVisualTransformation() }

        LaunchedEffect(saveState) {
            when (val state = saveState) {
                is SaveTransactionUiState.Success -> {
                    val message =
                        if (state.isEditMode) "Transaksi berhasil diperbarui!" else "Transaksi berhasil disimpan!"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (!state.isEditMode) { // Hanya clear field jika mode tambah
                        viewModel.clearInputFields()
                    }
                    viewModel.resetUiState()
                    onTransactionSavedSuccessfully() // Ini akan menavigasi kembali
                }

                is SaveTransactionUiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    // viewModel.resetUiState() // Pertimbangkan kapan harus mereset state error
                }

                else -> { /* Idle atau Loading */
                }
            }
        }

        // Efek untuk memberi tahu ViewModel ketika tipe transaksi berubah di UI
        LaunchedEffect(selectedTransactionType) {
            viewModel.onTransactionTypeChanged(selectedTransactionType)
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Text("TAMBAH TRANSAKSI") // Jika ingin menambahkan judul di sini
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            viewModel.attemptSaveOrUpdateTransaction() // Panggil fungsi baru/modifikasi
                        },
                        enabled = saveState !is SaveTransactionUiState.Loading,
                        modifier = Modifier
                            .width(346.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MyButtonBlackBlue,
                            contentColor = MyButtonOrange
                        )
                    ) {
                        if (saveState is SaveTransactionUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MyWhiteContent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            // Teks tombol dinamis
                            Text(
                                if (isEditMode) "UPDATE" else "SIMPAN",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(
                        topStart = mainContentCornerRadius,
                        topEnd = mainContentCornerRadius
                    ),
                    color = Color.White, // Buat Surface utama transparan agar shadow terlihat dan background dari Box bisa tembus jika ada
                    shadowElevation = 6.dp
                ) {
                    // Column utama yang mengatur tata letak vertikal untuk header dan input area
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Header Tab Hitam
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape( // Bentuk clip sama dengan Surface di atasnya
                                        topStart = mainContentCornerRadius,
                                        topEnd = mainContentCornerRadius
                                    )
                                )
                                .background(Color(0xFF2C383F))
                                .padding(horizontal = 24.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                // Judul dinamis di dalam header tab
                                text = if (isEditMode) "EDIT TRANSAKSI" else "TIPE TRANSAKSI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            )
                            val transactionTypes = TransactionType.values()
                            TabRow(
                                selectedTabIndex = selectedTransactionType.ordinal, // Dari ViewModel
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = Color.Transparent,
                                contentColor = Color.White,
                                indicator = { tabPositions ->
                                    TabRowDefaults.Indicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTransactionType.ordinal]),
                                        height = 3.dp,
                                        color = MyButtonOrange
                                    )
                                },
                                divider = {}
                            ) {
                                transactionTypes.forEachIndexed { index, type ->
                                    val isSelected = selectedTransactionType.ordinal == index
                                    val tabTextColor = if (isSelected) {
                                        MyButtonOrange
                                    } else {
                                        Color.LightGray.copy(alpha = 0.8f)
                                    }
                                    Tab(
                                        selected = isSelected,
                                        onClick = {
                                            // Langsung panggil fungsi ViewModel untuk mengubah tipe
                                            viewModel.onTransactionTypeChanged(type)
                                        },
                                        text = {
                                            Text(
                                                text = if (type == TransactionType.PEMASUKAN) "PEMASUKAN" else "PENGELUARAN",
                                                fontWeight = FontWeight.Medium,
                                                color = tabTextColor
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        val overlapAmount = 21.dp
                        val inputAreaMinHeight = 320.dp

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = -overlapAmount)
                                .defaultMinSize(minHeight = inputAreaMinHeight + overlapAmount)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = mainContentCornerRadius,
                                        topEnd = mainContentCornerRadius,
                                        bottomStart = 0.dp,
                                        bottomEnd = 0.dp
                                    )
                                )
                                .background(Color.White)
                                .padding(
                                    start = 24.dp,
                                    end = 24.dp,
                                    top = 15.dp + overlapAmount,
                                    bottom = 0.dp
                                )
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(26.dp)
                        ) {
                            // Logika tampilan field berdasarkan selectedTransactionType (dari ViewModel)
                            if (selectedTransactionType == TransactionType.PEMASUKAN) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = sellingPrice, // Ini tetap string angka murni dari ViewModel
                                        onValueChange = { newValue ->
                                            // Izinkan hanya digit. Nilai yang disimpan di ViewModel adalah angka murni.
                                            val digitsOnly = newValue.filter { it.isDigit() }
                                            // Batasi panjangnya jika perlu, misal maksimal 12 digit untuk menghindari Long overflow
                                            if (digitsOnly.length <= 12) {
                                                viewModel.onSellingPriceChange(digitsOnly)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Harga Jual (Rp)") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number, // Tetap KeyboardType.Number
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(onNext = {
                                            focusManager.moveFocus(FocusDirection.Right)
                                        }),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = numberDecimalVisualTransformation // Terapkan di sini
                                    )
                                    OutlinedTextField(
                                        value = amount, // Ini tetap string angka murni dari ViewModel
                                        onValueChange = { newValue ->
                                            val digitsOnly = newValue.filter { it.isDigit() }
                                            if (digitsOnly.length <= 12) { // Batas panjang yang sama
                                                viewModel.onAmountChange(digitsOnly)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Harga Modal (Rp)") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(onNext = {
                                            focusManager.moveFocus(FocusDirection.Down)
                                        }),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = numberDecimalVisualTransformation // Terapkan di sini
                                    )
                                }
// ...
                            } else { // PENGELUARAN
                                OutlinedTextField(
                                    value = amount, // Ini tetap string angka murni dari ViewModel
                                    onValueChange = { newValue ->
                                        val digitsOnly = newValue.filter { it.isDigit() }
                                        if (digitsOnly.length <= 12) { // Batas panjang yang sama
                                            viewModel.onAmountChange(digitsOnly)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Nominal (Rp)") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = {
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    visualTransformation = numberDecimalVisualTransformation // Terapkan di sini
                                )
                            }

                            OutlinedTextField(
                                value = description,
                                onValueChange = { viewModel.onDescriptionChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Deskripsi") },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences, // DITAMBAHKAN/DIUBAH
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = {
                                    focusManager.clearFocus()
                                    showDatePickerDialog = true
                                }),
                                singleLine = false,
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = transactionDateString,
                                onValueChange = { /* Dikelola oleh DatePicker -> ViewModel */ },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Kalendar (Tahun-Bulan-Tanggal)") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePickerDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarToday,
                                            contentDescription = "Pilih Tanggal"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )

//                            Spacer(modifier = Modifier.height(4.dp))

                            var isDebt by remember { mutableStateOf(false) }
                            var debtorName by remember { mutableStateOf("") }
                            var debtorPhone by remember { mutableStateOf("") }

                            // ====== Checkbox "Tandai sebagai hutang" ======
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = viewModel.isDebt.collectAsState().value,
                                    onCheckedChange = { checked ->
                                        viewModel.onIsDebtChanged(checked)
                                    }
                                )
                                Text("Tandai sebagai hutang", style = MaterialTheme.typography.bodyLarge)
                            }

                            if (viewModel.isDebt.collectAsState().value) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = viewModel.debtorName.collectAsState().value,
                                    onValueChange = { viewModel.onDebtorNameChanged(it) },
                                    label = { Text("Nama Pihak yang Berhutang") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = viewModel.debtorPhone.collectAsState().value,
                                    onValueChange = { viewModel.onDebtorPhoneChanged(it) },
                                    label = { Text("Nomor Telepon") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
                                )
                            }
                        }
                    }
                }
            }
        }

        // DatePickerDialog
        if (showDatePickerDialog) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val initialMillis = remember(transactionDateString) {
                try {
                    val parsedDate = sdf.parse(transactionDateString)
                    if (parsedDate != null) {
                        // Konversi tanggal yang diparsing (yang berada di timezone lokal 00:00)
                        // ke UTC midnight untuk tanggal tersebut.
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        val localCalendar = Calendar.getInstance()
                        localCalendar.time = parsedDate

                        calendar.set(
                            localCalendar.get(Calendar.YEAR),
                            localCalendar.get(Calendar.MONTH),
                            localCalendar.get(Calendar.DAY_OF_MONTH),
                            0, 0, 0
                        )
                        calendar.set(Calendar.MILLISECOND, 0)
                        calendar.timeInMillis
                    } else {
                        // Default ke hari ini, UTC midnight
                        val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        todayCalendar.set(Calendar.MINUTE, 0)
                        todayCalendar.set(Calendar.SECOND, 0)
                        todayCalendar.set(Calendar.MILLISECOND, 0)
                        todayCalendar.timeInMillis
                    }
                } catch (e: Exception) {
                    // Default ke hari ini, UTC midnight jika ada error
                    val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    todayCalendar.set(Calendar.MINUTE, 0)
                    todayCalendar.set(Calendar.SECOND, 0)
                    todayCalendar.set(Calendar.MILLISECOND, 0)
                    todayCalendar.timeInMillis
                }
            }

            // Log untuk debugging (opsional)
            // Log.d("DatePickerDebug", "Initial transactionDateString: $transactionDateString")
            // Log.d("DatePickerDebug", "Calculated initialMillis (UTC midnight): $initialMillis, Date: ${Date(initialMillis)}")

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialMillis
            )

            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePickerDialog = false
                            datePickerState.selectedDateMillis?.let { selectedUtcMillis ->
                                // selectedUtcMillis adalah UTC midnight.
                                // Kita perlu mengonversinya kembali ke tanggal lokal jika diperlukan
                                // atau cukup format langsung karena sdf akan menggunakan timezone default.
                                // Jika SDF menggunakan timezone default (lokal), maka akan benar.
                                val selectedCalendar =
                                    Calendar.getInstance() // Menggunakan timezone lokal
                                selectedCalendar.timeInMillis =
                                    selectedUtcMillis // Set dengan UTC millis

                                // Jika sdf menggunakan Locale.getDefault(), ia akan memformat tanggal
                                // berdasarkan timezone default perangkat dari UTC millis yang diberikan.
                                // Ini seharusnya sudah benar.
                                viewModel.onDateChange(sdf.format(selectedCalendar.time))
                            }
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) { Text("Batal") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }


    // ... Preview Composable (pastikan menggunakan nama Composable yang baru jika diubah)
    @Preview(showBackground = true, name = "AddEdit Default Preview")
    @Composable
    fun AddEditTransactionScreenDefaultPreview() {
        XKasTheme {
            AddEditTransactionScreen(
                onNavigateUp = {},
                onTransactionSavedSuccessfully = {}
                // Tidak perlu meneruskan viewModel jika hiltViewModel() bisa berjalan di Preview
                // Anda juga tidak meneruskan 'modifier' di sini, jadi akan menggunakan default Modifier
            )
        }
    }

    @Preview(showBackground = true, device = "spec:width=360dp,height=740dp,dpi=420")
    @Composable
    fun AddTransactionScreenPemasukanEditPreview() {
        XKasTheme {
            AddEditTransactionScreen(
                onNavigateUp = {},
                onTransactionSavedSuccessfully = { /* ... */ }
            )
        }
    }