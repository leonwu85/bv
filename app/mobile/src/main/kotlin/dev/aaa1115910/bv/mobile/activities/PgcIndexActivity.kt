package dev.aaa1115910.bv.mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.mobile.screen.PgcIndexScreen
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

class PgcIndexActivity : ComponentActivity() {
    companion object {
        fun actionStart(
            context: Context,
            pgcType: PgcType
        ) {
            context.startActivity(
                Intent(context, PgcIndexActivity::class.java).apply {
                    putExtra("pgcType", pgcType.ordinal)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BVMobileTheme {
                PgcIndexScreen()
            }
        }
    }
}
