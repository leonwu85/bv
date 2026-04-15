package dev.aaa1115910.biliapi.entity.pgc.index

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val PGC_INDEX_ORDER_FIELD = "order"

@Serializable
data class PgcIndexConditionData(
    @SerialName("filter")
    val filters: List<PgcIndexConditionFilter> = emptyList(),
    val order: List<PgcIndexConditionOrder> = emptyList()
) {
    fun buildSections(): List<PgcIndexSection> = buildList {
        if (order.isNotEmpty()) {
            add(
                PgcIndexSection(
                    field = PGC_INDEX_ORDER_FIELD,
                    title = "排序",
                    options = order.map {
                        PgcIndexOption(
                            field = PGC_INDEX_ORDER_FIELD,
                            keyword = it.field,
                            name = it.name,
                            sort = it.sort.substringBefore(',').ifBlank { "0" }
                        )
                    }
                )
            )
        }
        filters.forEach { filter ->
            if (filter.values.isNotEmpty()) {
                add(
                    PgcIndexSection(
                        field = filter.field,
                        title = filter.name,
                        options = filter.values.map { value ->
                            PgcIndexOption(
                                field = filter.field,
                                keyword = value.keyword,
                                name = value.name
                            )
                        }
                    )
                )
            }
        }
    }
}

@Serializable
data class PgcIndexConditionFilter(
    val field: String,
    val name: String,
    val values: List<PgcIndexConditionValue> = emptyList()
)

@Serializable
data class PgcIndexConditionOrder(
    val field: String,
    val name: String,
    val sort: String = "0"
)

@Serializable
data class PgcIndexConditionValue(
    val keyword: String,
    val name: String
)

data class PgcIndexSection(
    val field: String,
    val title: String,
    val options: List<PgcIndexOption>
)

data class PgcIndexOption(
    val field: String,
    val keyword: String,
    val name: String,
    val sort: String? = null
)