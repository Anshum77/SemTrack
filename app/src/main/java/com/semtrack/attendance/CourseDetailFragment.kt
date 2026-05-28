package com.semtrack.attendance

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.semtrack.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CourseDetailFragment : Fragment() {

    private val courseId: Long by lazy {
        requireArguments().getLong(ARG_COURSE_ID)
    }
    private val courseName: String by lazy {
        requireArguments().getString(ARG_COURSE_NAME).orEmpty()
    }

    private val viewModel: AttendanceDetailViewModel by viewModels {
        AttendanceDetailViewModel.Factory(requireContext().applicationContext, courseId)
    }

    private lateinit var tvCourseName: TextView
    private lateinit var tvPresentCount: TextView
    private lateinit var tvAbsent: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvSafeSkipsLabel: TextView
    private lateinit var tvSafeSkipsValue: TextView
    private lateinit var llSafeSkipsContainer: View
    private lateinit var ivSafeSkipsIcon: ImageView
    private lateinit var btnPickDate: MaterialButton
    private lateinit var btnPresent: MaterialButton
    private lateinit var btnAbsent: MaterialButton
    private lateinit var btnViewHistory: MaterialButton

    private val historyAdapter = AttendanceEntryAdapter(
        formatDate = { epochDay -> formatDate(epochDay) },
        onOptions = { entry -> showEntryOptions(entry) }
    )

    private var selectedDateEpochDay: Long = LocalDate.now().toEpochDay()
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_attendance_detail, container, false)

        tvCourseName = view.findViewById(R.id.tv_course_name)
        tvPresentCount = view.findViewById(R.id.tv_present_count)
        tvAbsent = view.findViewById(R.id.tv_absent_count)
        tvTotalCount = view.findViewById(R.id.tv_total_count)
        tvPercent = view.findViewById(R.id.tv_attendance_percent)
        tvSafeSkipsLabel = view.findViewById(R.id.tv_safe_skips_label)
        tvSafeSkipsValue = view.findViewById(R.id.tv_safe_skips_value)
        llSafeSkipsContainer = view.findViewById(R.id.ll_safe_skips_container)
        ivSafeSkipsIcon = view.findViewById(R.id.iv_safe_skips_icon)
        btnPickDate = view.findViewById(R.id.btn_pick_date)
        btnPresent = view.findViewById(R.id.btn_mark_present)
        btnAbsent = view.findViewById(R.id.btn_mark_absent)
        btnViewHistory = view.findViewById(R.id.btn_view_history)

        btnPickDate.setOnClickListener { showDatePicker() }
        btnPresent.setOnClickListener { markAttendance(AttendanceStatus.PRESENT) }
        btnAbsent.setOnClickListener { markAttendance(AttendanceStatus.ABSENT) }
        btnViewHistory.setOnClickListener { showHistoryBottomSheet() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCourseName.text = courseName
        updateSelectedDateText()
        (activity as? AppCompatActivity)?.supportActionBar?.title = courseName

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.entries.collect { entries ->
                        historyAdapter.submitEntries(entries)
                    }
                }
                launch {
                    viewModel.stats.collect { stats ->
                        val total = stats.total
                        val present = stats.present
                        val absent = total - present
                        val percent = stats.percent

                        // Modern animation: Smoothly count up percentage
                        val oldPercent = tvPercent.tag as? Int ?: 0
                        ValueAnimator.ofInt(oldPercent, percent).apply {
                            duration = 800
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { animator ->
                                tvPercent.text = getString(R.string.attendance_percent_value, animator.animatedValue as Int)
                            }
                            start()
                        }
                        tvPercent.tag = percent

                        tvPresentCount.text = present.toString()
                        tvAbsent.text = absent.toString()
                        tvTotalCount.text = total.toString()

                        // Modern animation: Smoothly fade colors between thresholds
                        val oldColor = tvPercent.currentTextColor
                        val newColor = if (percent >= 75) {
                            ContextCompat.getColor(requireContext(), R.color.attendance_green)
                        } else {
                            ContextCompat.getColor(requireContext(), R.color.attendance_red)
                        }
                        if (oldColor != newColor && oldPercent != 0) {
                            ValueAnimator.ofObject(ArgbEvaluator(), oldColor, newColor).apply {
                                duration = 800
                                addUpdateListener { animator -> tvPercent.setTextColor(animator.animatedValue as Int) }
                                start()
                            }
                        } else {
                            tvPercent.setTextColor(newColor)
                        }

                        if (total == 0) {
                            tvSafeSkipsLabel.text = getString(R.string.attendance_safe_skips_label)
                            tvSafeSkipsValue.text = "0"
                            llSafeSkipsContainer.setBackgroundResource(R.drawable.bg_safe_skips_card)
                            ivSafeSkipsIcon.setImageResource(R.drawable.ic_shield_check)
                        } else if (percent >= 75) {
                            tvSafeSkipsLabel.text = getString(R.string.attendance_safe_skips_label)
                            val safeSkips = (4 * present) / 3 - total
                            tvSafeSkipsValue.text = safeSkips.toString()
                            llSafeSkipsContainer.setBackgroundResource(R.drawable.bg_safe_skips_card)
                            ivSafeSkipsIcon.setImageResource(R.drawable.ic_shield_check)
                        } else {
                            tvSafeSkipsLabel.text = getString(R.string.attendance_required_classes_label)
                            val requiredClasses = 3 * total - 4 * present
                            tvSafeSkipsValue.text = requiredClasses.toString()
                            llSafeSkipsContainer.setBackgroundResource(R.drawable.bg_required_classes_card)
                            ivSafeSkipsIcon.setImageResource(R.drawable.ic_warning_triangle)
                        }
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val todayMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val selectedMillis = LocalDate.ofEpochDay(selectedDateEpochDay)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(selectedMillis)
            .setCalendarConstraints(constraints)
            .setTitleText(getString(R.string.attendance_pick_date_title))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            if (selection != null) {
                val date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                selectedDateEpochDay = date.toEpochDay()
                updateSelectedDateText()
            }
        }

        picker.show(childFragmentManager, "attendanceDatePicker")
    }

    private fun updateSelectedDateText() {
        btnPickDate.text = formatDate(selectedDateEpochDay)
    }

    private fun markAttendance(status: Int) {
        viewModel.markAttendance(selectedDateEpochDay, status) { result ->
            when (result) {
                AttendanceDetailViewModel.MarkResult.Inserted -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.attendance_marked_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                AttendanceDetailViewModel.MarkResult.AlreadyMarked -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.attendance_already_marked),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is AttendanceDetailViewModel.MarkResult.DifferentStatus -> {
                    val existingLabel = statusLabel(result.existingStatus)
                    val newLabel = statusLabel(status)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.attendance_change_title)
                        .setMessage(
                            getString(
                                R.string.attendance_change_message,
                                existingLabel,
                                newLabel
                            )
                        )
                        .setPositiveButton("Change") { dialog, _ ->
                            viewModel.updateAttendance(result.entryId, status)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }

    private fun showEntryOptions(entry: AttendanceEntryUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.attendance_entry_options_title)
            .setItems(arrayOf(getString(R.string.attendance_entry_delete))) { dialog, _ ->
                showDeleteEntryDialog(entry)
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteEntryDialog(entry: AttendanceEntryUi) {
        val dateText = formatDate(entry.dateEpochDay)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.attendance_delete_title)
            .setMessage(getString(R.string.attendance_delete_message, dateText))
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteEntry(entry.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showHistoryBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_attendance_history, null)
        val recycler = sheetView.findViewById<RecyclerView>(R.id.rv_history_sheet)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = historyAdapter
        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun statusLabel(status: Int): String {
        return if (status == AttendanceStatus.PRESENT) {
            getString(R.string.attendance_status_present)
        } else {
            getString(R.string.attendance_status_absent)
        }
    }

    private fun formatDate(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(dateFormatter)
    }

    private class AttendanceEntryAdapter(
        private val formatDate: (Long) -> String,
        private val onOptions: (AttendanceEntryUi) -> Unit
    ) : RecyclerView.Adapter<AttendanceEntryAdapter.EntryViewHolder>() {

        private var entries: List<AttendanceEntryUi> = emptyList()

        class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val date: TextView = view.findViewById(R.id.tv_entry_date)
            val statusText: TextView = view.findViewById(R.id.tv_entry_status)
            val statusChip: MaterialCardView = view.findViewById(R.id.chip_status)
            val options: ImageView = view.findViewById(R.id.btn_entry_options)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attendance_entry, parent, false)
            return EntryViewHolder(view)
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val entry = entries[position]
            holder.date.text = formatDate(entry.dateEpochDay)
            val isPresent = entry.status == AttendanceStatus.PRESENT
            holder.statusText.text = if (isPresent) {
                holder.itemView.context.getString(R.string.attendance_status_present)
            } else {
                holder.itemView.context.getString(R.string.attendance_status_absent)
            }

            val bgColor = if (isPresent) {
                MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorTertiaryContainer)
            } else {
                MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorErrorContainer)
            }
            val textColor = if (isPresent) {
                MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnTertiaryContainer)
            } else {
                MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnErrorContainer)
            }
            holder.statusChip.setCardBackgroundColor(bgColor)
            holder.statusText.setTextColor(textColor)

            holder.options.setOnClickListener {
                onOptions(entry)
            }
        }

        override fun getItemCount(): Int = entries.size

        fun submitEntries(newEntries: List<AttendanceEntryUi>) {
            entries = newEntries
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_COURSE_NAME = "course_name"

        fun newInstance(courseId: Long, courseName: String): CourseDetailFragment {
            val fragment = CourseDetailFragment()
            fragment.arguments = Bundle().apply {
                putLong(ARG_COURSE_ID, courseId)
                putString(ARG_COURSE_NAME, courseName)
            }
            return fragment
        }
    }
}
