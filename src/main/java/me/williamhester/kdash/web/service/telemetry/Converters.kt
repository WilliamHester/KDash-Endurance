package me.williamhester.kdash.web.service.telemetry

import me.williamhester.kdash.enduranceweb.proto.QueryResult
import me.williamhester.kdash.enduranceweb.proto.listValue
import me.williamhester.kdash.enduranceweb.proto.queryResult
import me.williamhester.kdash.web.service.telemetry.query.DataPointValue
import me.williamhester.kdash.web.service.telemetry.query.ListValue
import me.williamhester.kdash.web.service.telemetry.query.ScalarValue

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