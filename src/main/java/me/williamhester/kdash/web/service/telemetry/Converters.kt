package me.williamhester.kdash.web.service.telemetry

import me.williamhester.kdash.enduranceweb.proto.QueryResult
import me.williamhester.kdash.enduranceweb.proto.listValue
import me.williamhester.kdash.enduranceweb.proto.queryResult
import me.williamhester.kdash.web.models.DataPointValue
import me.williamhester.kdash.web.models.ListValue
import me.williamhester.kdash.web.models.ScalarValue

internal object Converters {
  fun DataPointValue.toQueryResult(): QueryResult {
    return queryResult {
      when (this@toQueryResult) {
        is ScalarValue -> this@queryResult.scalar = this@toQueryResult.value
        is ListValue -> this@queryResult.list = listValue {
          this@listValue.values += this@toQueryResult.value
        }
      }
    }
  }
}