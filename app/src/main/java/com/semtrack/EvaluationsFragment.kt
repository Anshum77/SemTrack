package com.semtrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.semtrack.evaluations.CourseEvaluationUi
import com.semtrack.evaluations.EvaluationsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class EvaluationsFragment : Fragment() {

    private val viewModel: EvaluationsViewModel by viewModels {
        EvaluationsViewModel.Factory(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_evaluations, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_courses)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = CourseEvaluationAdapter(
            onCourseClick = { course ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, com.semtrack.evaluations.EvaluationDetailFragment.newInstance(course.id, course.name))
                    .addToBackStack(null)
                    .commit()
            },
            onManageCourse = { course ->
                showCourseOptionsDialog(course)
            }
        )
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                adapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            
            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                viewModel.updateCourseOrders(adapter.getCurrentCourses().map { it.id })
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.courses.collect { courses ->
                    currentCourses = courses
                    adapter.submitCourses(courses)
                }
            }
        }

        view.findViewById<View>(R.id.fab_add_course).setOnClickListener {
            showAddCourseDialog()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = "Evaluations"
    }

    private var currentCourses: List<CourseEvaluationUi> = emptyList()

    private fun showAddCourseDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_course, null)
        val inputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_course_name)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_course_name)
        editText.setText("")

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_course_title))
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = editText.text?.toString()?.trim().orEmpty()
                when {
                    name.isBlank() -> {
                        inputLayout.error = getString(R.string.course_name_required)
                    }
                    !isCourseNameAvailable(name) -> {
                        inputLayout.error = getString(R.string.course_name_duplicate)
                    }
                    else -> {
                        inputLayout.error = null
                        viewModel.addCourse(name)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showCourseOptionsDialog(course: CourseEvaluationUi) {
        val options = arrayOf(
            getString(R.string.course_option_rename),
            getString(R.string.course_option_delete)
        )

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(course.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showRenameCourseDialog(course)
                    1 -> showDeleteCourseDialog(course)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showRenameCourseDialog(course: CourseEvaluationUi) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_course, null)
        val inputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_course_name)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_course_name)
        editText.setText(course.name)
        editText.setSelection(course.name.length)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.rename_course_title))
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = editText.text?.toString()?.trim().orEmpty()
                when {
                    name.isBlank() -> {
                        inputLayout.error = getString(R.string.course_name_required)
                    }
                    !isCourseNameAvailable(name, course.id) -> {
                        inputLayout.error = getString(R.string.course_name_duplicate)
                    }
                    else -> {
                        inputLayout.error = null
                        viewModel.renameCourse(course.id, name)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteCourseDialog(course: CourseEvaluationUi) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_course_title)
            .setMessage(R.string.delete_course_message)
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteCourse(course.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun isCourseNameAvailable(name: String, excludeCourseId: Long? = null): Boolean {
        return currentCourses.none {
            it.id != excludeCourseId && it.name.equals(name, ignoreCase = true)
        }
    }

    private class CourseEvaluationAdapter(
        private val onCourseClick: (CourseEvaluationUi) -> Unit,
        private val onManageCourse: (CourseEvaluationUi) -> Unit
    ) : RecyclerView.Adapter<CourseEvaluationAdapter.ViewHolder>() {

        private var courses: List<CourseEvaluationUi> = emptyList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_course_name)
            val options: ImageView = view.findViewById(R.id.btn_course_options)
            val percent: TextView = view.findViewById(R.id.tv_attendance_percent)
            val classes: TextView = view.findViewById(R.id.tv_attendance_classes)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val course = courses[position]
            holder.name.text = course.name
            
            holder.options.setOnClickListener {
                onManageCourse(course)
            }

            val evalFormatted = String.format(Locale.getDefault(), "%.2f%%", course.totalEvaluatedWeightage)
            val obtFormatted = String.format(Locale.getDefault(), "%.2f%%", course.totalObtainedPercentage)
            
            holder.percent.text = obtFormatted
            holder.classes.text = "out of $evalFormatted evaluated"
            holder.percent.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.attendance_green))

            holder.itemView.setOnClickListener {
                onCourseClick(course)
            }
        }

        override fun getItemCount(): Int = courses.size

        fun submitCourses(newCourses: List<CourseEvaluationUi>) {
            courses = newCourses
            notifyDataSetChanged()
        }

        fun moveItem(fromPosition: Int, toPosition: Int) {
            val list = courses.toMutableList()
            val item = list.removeAt(fromPosition)
            list.add(toPosition, item)
            courses = list
            notifyItemMoved(fromPosition, toPosition)
        }

        fun getCurrentCourses(): List<CourseEvaluationUi> = courses
    }
}