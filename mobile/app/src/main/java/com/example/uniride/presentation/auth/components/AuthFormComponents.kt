package com.example.uniride.presentation.auth.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.uniride.R
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.PasswordValidationError

@StringRes
fun EmailValidationError.messageRes(): Int = when (this) {
    EmailValidationError.BLANK -> R.string.auth_error_email_blank
    EmailValidationError.INVALID_FORMAT -> R.string.auth_error_email_format
    EmailValidationError.INVALID_DOMAIN -> R.string.auth_error_email_domain
}

@StringRes
fun PasswordValidationError.messageRes(): Int = when (this) {
    PasswordValidationError.BLANK -> R.string.auth_error_password_blank
    PasswordValidationError.TOO_SHORT -> R.string.auth_error_password_short
    PasswordValidationError.TOO_LONG -> R.string.auth_error_password_long
    PasswordValidationError.MISSING_LETTER -> R.string.auth_error_password_letter
    PasswordValidationError.MISSING_DIGIT -> R.string.auth_error_password_digit
}

/** Password field with visibility toggle and accessible contentDescription (RF01, RNF01). */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val bottomText = error ?: supportingText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = bottomText?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = keyboardActions,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    painter = painterResource(
                        if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                    ),
                    contentDescription = stringResource(
                        if (visible) R.string.auth_password_hide else R.string.auth_password_show,
                    ),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
