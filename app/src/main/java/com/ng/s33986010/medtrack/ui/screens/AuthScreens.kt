package com.ng.s33986010.medtrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.s33986010.medtrack.R
import com.ng.s33986010.medtrack.viewmodel.AuthState
import com.ng.s33986010.medtrack.viewmodel.AuthViewModel

// WELCOME SCREEN

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onSignUpClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.medtrack_logo),
            contentDescription = "MedTrack Logo",
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp)
        )
        Text("MedTrack Pro", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ng Li Xian (33986010)", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "This app is for tracking purposes only and does not replace professional medical advice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) { Text("Login") }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onSignUpClick, modifier = Modifier.fillMaxWidth()) { Text("Sign Up") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// LOGIN SCREEN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onClaimAccountClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var patientId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            errorMsg = (authState as AuthState.Error).message
            authViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Login") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMsg.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMsg, modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            OutlinedTextField(value = patientId, onValueChange = { patientId = it; errorMsg = "" },
                label = { Text("Patient ID") }, placeholder = { Text("e.g. P1001") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(
                value = password, onValueChange = { password = it; errorMsg = "" },
                label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "")
                    }
                }
            )

            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(onClick = { authViewModel.login(patientId, password) },
                    modifier = Modifier.fillMaxWidth()) { Text("Login") }
            }

            HorizontalDivider()
            Text("New to MedTrack?", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            OutlinedButton(onClick = onClaimAccountClick, modifier = Modifier.fillMaxWidth()) {
                Text("Claim Existing Account")
            }
            TextButton(onClick = onSignUpClick, modifier = Modifier.fillMaxWidth()) {
                Text("Create New Account")
            }
        }
    }
}

// CLAIM ACCOUNT SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimAccountScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    val authState by authViewModel.authState.collectAsState()
    var patientId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            errorMsg = (authState as AuthState.Error).message
            authViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Claim Account") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text("Enter your Patient ID and Phone Number from the database to claim your account and set a password.",
                    modifier = Modifier.padding(12.dp))
            }

            if (errorMsg.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMsg, modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            OutlinedTextField(value = patientId, onValueChange = { patientId = it; errorMsg = "" },
                label = { Text("Patient ID *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = phone, onValueChange = { phone = it; errorMsg = "" },
                label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

            OutlinedTextField(
                value = password, onValueChange = { password = it; errorMsg = "" },
                label = { Text("New Password *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "")
                    }
                }
            )
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; errorMsg = "" },
                label = { Text("Confirm Password *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))

            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            errorMsg = "Passwords do not match"
                        } else {
                            authViewModel.claimAccount(patientId, phone, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Claim Account") }
            }
        }
    }
}

// SIGN UP SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    val authState by authViewModel.authState.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            errorMsg = (authState as AuthState.Error).message
            authViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sign Up") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMsg.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMsg, modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it; errorMsg = "" },
                label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = phone, onValueChange = { phone = it; errorMsg = "" },
                label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(
                value = password, onValueChange = { password = it; errorMsg = "" },
                label = { Text("Password *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "")
                    }
                }
            )
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; errorMsg = "" },
                label = { Text("Confirm Password *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))

            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(onClick = { authViewModel.signUp(name, phone, password, confirmPassword) },
                    modifier = Modifier.fillMaxWidth()) { Text("Create Account") }
            }
        }
    }
}
