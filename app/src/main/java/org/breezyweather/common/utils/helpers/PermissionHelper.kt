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

package org.breezyweather.common.utils.helpers

import android.app.Activity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import org.breezyweather.main.utils.StatementManager

object PermissionHelper {

    /**
     * Tries to request a permission via the default permission dialog and falls back to a custom action if the default
     * dialog cannot be launched.
     * // TODO: add description
     * Source: https://stackoverflow.com/a/50639402
     */
    fun requestPermissionWithFallback(
        activity: Activity,
        permission: String,
        requestCode: Int = 0,
        statementManager: StatementManager,
        requestPermissionFallback: () -> Unit, // TODO: improve wording
    ) {
        val showRationale = shouldShowRequestPermissionRationale(activity, permission)
        val permissionDenied = statementManager.isPermissionDenied(permission)

        if (!showRationale && permissionDenied) {
            requestPermissionFallback()
        } else {
            requestPermissions(
                activity,
                arrayOf(permission),
                requestCode
            )

            if (showRationale && !permissionDenied) {
                statementManager.setPermissionDenied(permission)
            }
        }
    }
}
