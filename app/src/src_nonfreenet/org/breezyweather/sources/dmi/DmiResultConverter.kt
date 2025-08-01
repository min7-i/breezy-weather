/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.sources.dmi

import android.graphics.Color
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.Precipitation
import breezyweather.domain.weather.model.Wind
import breezyweather.domain.weather.reference.AlertSeverity
import breezyweather.domain.weather.reference.WeatherCode
import breezyweather.domain.weather.wrappers.DailyWrapper
import breezyweather.domain.weather.wrappers.HourlyWrapper
import breezyweather.domain.weather.wrappers.TemperatureWrapper
import org.breezyweather.common.extensions.getIsoFormattedDate
import org.breezyweather.common.extensions.toDateNoHour
import org.breezyweather.sources.dmi.json.DmiResult
import org.breezyweather.sources.dmi.json.DmiTimeserie
import org.breezyweather.sources.dmi.json.DmiWarning
import java.util.Objects

internal fun convert(
    location: Location,
    result: DmiResult,
): Location {
    return location.copy(
        cityId = result.id,
        timeZone = result.timezone!!,
        country = result.country ?: location.country,
        countryCode = result.country,
        admin1 = "",
        admin1Code = "",
        admin2 = "",
        admin2Code = "",
        admin3 = "",
        admin3Code = "",
        admin4 = "",
        admin4Code = "",
        city = result.city ?: ""
    )
}

/**
 * Returns empty daily forecast
 * Will be completed with hourly forecast data later
 */
internal fun getDailyForecast(
    location: Location,
    dailyResult: List<DmiTimeserie>?,
): List<DailyWrapper> {
    if (dailyResult.isNullOrEmpty()) return emptyList()

    val dailyList = mutableListOf<DailyWrapper>()
    val hourlyListByDay = dailyResult.groupBy {
        it.localTimeIso.getIsoFormattedDate(location)
    }
    for (i in 0 until hourlyListByDay.entries.size - 1) {
        val dayDate = hourlyListByDay.keys.toTypedArray()[i].toDateNoHour(location.javaTimeZone)
        if (dayDate != null) {
            dailyList.add(
                DailyWrapper(
                    date = dayDate
                )
            )
        }
    }
    return dailyList
}

/**
 * Returns hourly forecast
 */
internal fun getHourlyForecast(
    hourlyResult: List<DmiTimeserie>?,
): List<HourlyWrapper> {
    if (hourlyResult.isNullOrEmpty()) return emptyList()

    return hourlyResult.map { result ->
        HourlyWrapper(
            // TODO: Check units
            date = result.localTimeIso,
            weatherCode = getWeatherCode(result.symbol),
            temperature = TemperatureWrapper(
                temperature = result.temp
            ),
            precipitation = if (result.precip != null) {
                Precipitation(total = result.precip)
            } else {
                null
            },
            wind = Wind(
                degree = result.windDegree,
                speed = result.windSpeed,
                gusts = result.windGust
            ),
            relativeHumidity = result.humidity,
            pressure = result.pressure,
            visibility = result.visibility
        )
    }
}

internal fun getAlertList(
    resultList: List<DmiWarning>?,
): List<Alert>? {
    if (resultList.isNullOrEmpty()) return null
    return resultList.map {
        Alert(
            alertId = Objects.hash(it.warningTitle, it.validFrom).toString(),
            startDate = it.validFrom,
            endDate = it.validTo,
            headline = it.warningTitle,
            description = it.warningText,
            instruction = it.additionalText,
            source = "DMI",
            severity = when (it.formattedCategory) {
                3 -> AlertSeverity.EXTREME
                2 -> AlertSeverity.SEVERE
                1 -> AlertSeverity.MODERATE
                0 -> AlertSeverity.MINOR
                else -> AlertSeverity.UNKNOWN
            },
            color = when (it.formattedCategory) {
                3 -> Color.rgb(204, 31, 31)
                2 -> Color.rgb(254, 142, 82)
                1 -> Color.rgb(255, 217, 3)
                0 -> Color.rgb(146, 208, 245)
                else -> Alert.colorFromSeverity(AlertSeverity.UNKNOWN)
            }
        )
    }
}

private fun getWeatherCode(icon: Int?): WeatherCode? {
    return when (icon) {
        1, 101 -> WeatherCode.CLEAR
        2, 102 -> WeatherCode.PARTLY_CLOUDY
        3, 103 -> WeatherCode.CLOUDY
        60, 63, 80, 160, 163, 180, 181 -> WeatherCode.RAIN
        168, 169, 183, 184 -> WeatherCode.SLEET
        70, 138, 170, 173, 185, 186 -> WeatherCode.SNOW
        195 -> WeatherCode.THUNDERSTORM
        145 -> WeatherCode.FOG
        else -> null
    }
}
