package com.semtrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.semtrack.attendance.AttendanceViewModel
import com.semtrack.attendance.CourseDetailFragment
import com.semtrack.attendance.CourseUi
import kotlinx.coroutines.launch

class AttendanceFragment : Fragment() {

    private val viewModel: AttendanceViewModel by viewModels {
        AttendanceViewModel.Factory(requireContext().applicationContext)
    }

    private lateinit var rvCourses: RecyclerView
    private lateinit var fabAddCourse: FloatingActionButton
    private lateinit var adapter: CourseAdapter
    private var currentCourses: List<CourseUi> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attendance, container, false)

        rvCourses = view.findViewById(R.id.rv_courses)
        fabAddCourse = view.findViewById(R.id.fab_add_course)

        adapter = CourseAdapter(
            onCourseClick = { course ->
                openCourseDetail(course)
            },
            onManageCourse = { course ->
                showCourseOptionsDialog(course)
            }
        )
        rvCourses.layoutManager = LinearLayoutManager(requireContext())
        rvCourses.adapter = adapter

        fabAddCourse.setOnClickListener {
            showAddCourseDialog()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.courses.collect { courses ->
                    currentCourses = courses
                    adapter.submitCourses(courses)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.attendance_title)
    }

    private fun showAddCourseDialog() {
        showCourseDialog(
            title = getString(R.string.add_course_title),
            defaultName = "",
            onSave = { name, onResult ->
                viewModel.addCourse(name, onResult)
            }
        )
    }

    private fun showRenameCourseDialog(course: CourseUi) {
        showCourseDialog(
            title = getString(R.string.rename_course_title),
            defaultName = course.name,
            onSave = { name, onResult ->
                viewModel.renameCourse(course.id, name, onResult)
            },
            excludeCourseId = course.id
        )
    }

    private fun showCourseDialog(
        title: String,
        defaultName: String,
        onSave: (String, (Boolean) -> Unit) -> Unit,
        excludeCourseId: Long? = null
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_course, null)
        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.til_course_name)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.et_course_name)
        editText.setText(defaultName)
        editText.setSelection(defaultName.length)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
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
                    !isCourseNameAvailable(name, excludeCourseId) -> {
                        inputLayout.error = getString(R.string.course_name_duplicate)
                    }
                    else -> {
                        inputLayout.error = null
                        onSave(name) { success ->
                            if (success) {
                                dialog.dismiss()
                            } else {
                                inputLayout.error = getString(R.string.course_name_duplicate)
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showCourseOptionsDialog(course: CourseUi) {
        val options = arrayOf(
            getString(R.string.course_option_rename),
            getString(R.string.course_option_delete)
        )

        MaterialAlertDialogBuilder(requireContext())
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

    private fun showDeleteCourseDialog(course: CourseUi) {
        MaterialAlertDialogBuilder(requireContext())
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

    private fun isCourseNameAvailable(name: String, excludeCourseId: Long?): Boolean {
        return currentCourses.none {
            it.id != excludeCourseId && it.name.equals(name, ignoreCase = true)
        }
    }

    private fun openCourseDetail(course: CourseUi) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CourseDetailFragment.newInstance(course.id, course.name))
            .addToBackStack(null)
            .commit()
    }

    private class CourseAdapter(
        private val onCourseClick: (CourseUi) -> Unit,
        private val onManageCourse: (CourseUi) -> Unit
    ) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

        private var courses: List<CourseUi> = emptyList()

        class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_course_name)
            val options: ImageView = view.findViewById(R.id.btn_course_options)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course_card, parent, false)
            return CourseViewHolder(view)
        }

        override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
            val course = courses[position]
            holder.name.text = course.name
            holder.options.setOnClickListener {
                onManageCourse(course)
            }
            holder.itemView.setOnClickListener {
                onCourseClick(course)
            }
        }

        override fun getItemCount(): Int = courses.size

        fun submitCourses(newCourses: List<CourseUi>) {
            courses = newCourses
            notifyDataSetChanged()
        }
    }
}