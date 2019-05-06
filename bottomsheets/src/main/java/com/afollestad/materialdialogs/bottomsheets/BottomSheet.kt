/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.materialdialogs.bottomsheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.main.DialogLayout
import com.afollestad.materialdialogs.utils.MDUtil.getWidthAndHeight
import com.afollestad.materialdialogs.utils.MDUtil.waitForHeight
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import java.lang.Float.isNaN
import kotlin.math.abs
import kotlin.math.min

/** @author Aidan Follestad (@afollestad) */
class BottomSheet : DialogBehavior {
  internal var bottomSheetBehavior: BottomSheetBehavior<*>? = null
  internal var bottomSheetView: ViewGroup? = null

  internal var minimumPeekHeight: Int? = null
  internal var minimumPeekHeightRatio: Float? = null

  private var rootView: CoordinatorLayout? = null
  private var dialog: MaterialDialog? = null

  @SuppressLint("InflateParams")
  override fun createView(
    context: Context,
    window: Window,
    layoutInflater: LayoutInflater,
    dialog: MaterialDialog
  ): ViewGroup {
    rootView = layoutInflater.inflate(
        R.layout.md_dialog_base_bottomsheet,
        null,
        false
    ) as CoordinatorLayout

    this.dialog = dialog
    this.bottomSheetView = rootView!!.findViewById(R.id.md_root_bottom_sheet)

    val (_, windowHeight) = window.windowManager.getWidthAndHeight()
    val desiredPeekHeight = if (minimumPeekHeightRatio != null) {
      (windowHeight * minimumPeekHeightRatio!!).toInt()
    } else {
      minimumPeekHeight
    } ?: (windowHeight * DEFAULT_PEEK_HEIGHT_RATIO).toInt()

    setupBottomSheetBehavior(desiredPeekHeight)
    bottomSheetView?.waitForHeight {
      if (this.measuredHeight >= desiredPeekHeight) {
        bottomSheetBehavior?.animatePeekHeight(
            dest = min(desiredPeekHeight, windowHeight),
            duration = LAYOUT_PEEK_CHANGE_DURATION_MS
        )
      } else {
        bottomSheetBehavior?.animatePeekHeight(
            dest = min(this.measuredHeight, desiredPeekHeight),
            duration = LAYOUT_PEEK_CHANGE_DURATION_MS
        )
      }
    }

    return rootView!!
  }

  private fun setupBottomSheetBehavior(minPeekHeight: Int) {
    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        .apply {
          isHideable = true
          peekHeight = minPeekHeight
        }
    bottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetCallback() {
      override fun onSlide(
        view: View,
        dY: Float
      ) {
        val percentage = if (isNaN(dY)) 0f else abs(dY)
        val peekHeight = bottomSheetBehavior?.peekHeight ?: return
        val diff = peekHeight * percentage
        val currentHeight = peekHeight - diff
        Log.d(
            "BottomSheet", "onSlide($dY)... " +
            "peekHeight = ${bottomSheetBehavior?.peekHeight}, " +
            "currentHeight = $currentHeight, " +
            "bottomSheetView.height = ${bottomSheetView?.measuredHeight}"
        )
      }

      override fun onStateChanged(
        view: View,
        state: Int
      ) {
        if (state == STATE_HIDDEN) {
          dialog?.dismiss()
          dialog = null
        }
      }
    })
  }

  override fun getDialogLayout(root: ViewGroup): DialogLayout {
    return root.findViewById(R.id.md_root) as DialogLayout
  }

  override fun setWindowConstraints(
    context: Context,
    window: Window,
    view: DialogLayout,
    maxWidth: Int?
  ) {
    if (maxWidth == 0) {
      // Postpone
      return
    }
    window.setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    val lp = LayoutParams()
        .apply {
          copyFrom(window.attributes)
          width = LayoutParams.MATCH_PARENT
          height = LayoutParams.MATCH_PARENT
        }
    window.attributes = lp
  }

  override fun setBackgroundColor(
    context: Context,
    window: Window,
    view: DialogLayout,
    color: Int,
    cornerRounding: Float
  ) {
    window.setBackgroundDrawable(null)
    bottomSheetView?.background = GradientDrawable().apply {
      cornerRadii = floatArrayOf(
          cornerRounding, cornerRounding, // top left
          cornerRounding, cornerRounding, // top right
          0f, 0f, // bottom left
          0f, 0f // bottom right
      )
      setColor(color)
    }
  }

  override fun onShow() {
    rootView?.setOnClickListener {
      if (dialog?.cancelOnTouchOutside == true) {
        // Clicking outside the bottom sheet dismisses the dialog
        dialog!!.dismiss()
      }
    }
  }

  override fun onDismiss(): Boolean {
    if (dialog != null &&
        bottomSheetBehavior != null &&
        bottomSheetBehavior!!.state != STATE_HIDDEN
    ) {
      bottomSheetBehavior!!.state = STATE_HIDDEN
      bottomSheetBehavior = null
      bottomSheetView = null
      rootView = null
      return true
    }
    return false
  }

  private companion object {
    private const val DEFAULT_PEEK_HEIGHT_RATIO = 0.6f
    private const val LAYOUT_PEEK_CHANGE_DURATION_MS = 1000L
  }
}
