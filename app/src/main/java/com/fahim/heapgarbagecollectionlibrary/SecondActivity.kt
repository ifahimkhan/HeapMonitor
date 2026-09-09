package com.fahim.heapgarbagecollectionlibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fahim.heapgarbagecollectionlibrary.ui.theme.HeapGarbageCollectionLibraryTheme

/** Second screen: proves the overlay attaches to every activity, not only the launcher. */
class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeapGarbageCollectionLibraryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RowList(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun RowList(modifier: Modifier = Modifier) {
    val rows = (1..ROW_COUNT).map { "Row $it" }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(rows) { label ->
            Text(
                text = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

private const val ROW_COUNT = 200
