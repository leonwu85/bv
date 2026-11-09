package dev.aaa1115910.bv.mobile.component.search

import androidx.compose.runtime.Composable
import dev.aaa1115910.bv.mobile.component.GaiaVerificationDialog
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel

@Composable
fun SearchVerification(viewModel: SearchResultViewModel) {
    val pending = viewModel.verification ?: return
    GaiaVerificationDialog(pending.voucher, viewModel::onGaiaVerified, viewModel::cancelVerification)
}
