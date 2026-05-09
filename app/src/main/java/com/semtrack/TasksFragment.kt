package com.semtrack

import android.os.Bundle
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

data class Task(
    val title: String,
    var isStarred: Boolean = false,
    var isCompleted: Boolean = false
)

data class TaskListState(
    val active: MutableList<Task> = mutableListOf(),
    val completed: MutableList<Task> = mutableListOf(),
    var isCompletedExpanded: Boolean = false
)

class TasksFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var tabLayout: TabLayout
    private lateinit var btnAddList: ImageView

    // Map to hold different categories of tasks. Using LinkedHashMap to preserve order.
    private val taskLists = LinkedHashMap<String, TaskListState>().apply {
        put("My Tasks", TaskListState())
    }

    private lateinit var pagerAdapter: ListsPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        viewPager = view.findViewById(R.id.view_pager)
        fabAddTask = view.findViewById(R.id.fab_add_task)
        tabLayout = view.findViewById(R.id.tab_layout)
        btnAddList = view.findViewById(R.id.btn_add_list)

        pagerAdapter = ListsPagerAdapter()
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = taskLists.keys.toList()[position]
        }.attach()

        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        btnAddList.setOnClickListener {
            showListDialog("New List", "") { newName ->
                if (newName.isNotBlank() && !taskLists.containsKey(newName)) {
                    taskLists[newName] = TaskListState()
                    pagerAdapter.notifyItemInserted(taskLists.size - 1)
                    viewPager.setCurrentItem(taskLists.size - 1, true)
                }
            }
        }

        return view
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null)
        val etTaskTitle = dialogView.findViewById<EditText>(R.id.et_task_title)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val title = etTaskTitle.text.toString().trim()
                if (title.isNotBlank()) {
                    val listKeyName = taskLists.keys.toList()[viewPager.currentItem]
                    taskLists[listKeyName]?.active?.add(0, Task(title))
                    pagerAdapter.notifyItemChanged(viewPager.currentItem)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showListDialog(title: String, defaultName: String, onSave: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            setText(defaultName)
            setSingleLine()
            setSelection(defaultName.length)
        }
        val margin = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(requireContext()).apply {
            setPadding(margin, margin, margin, margin)
            addView(editText)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                onSave(editText.text.toString().trim())
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteCompletedDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_completed_title)
            .setMessage(R.string.delete_completed_message)
            .setPositiveButton("Delete") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Inner adapter for ViewPager2
    inner class ListsPagerAdapter : RecyclerView.Adapter<ListsPagerAdapter.ListPageViewHolder>() {

        inner class ListPageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvListTitle: TextView = view.findViewById(R.id.tv_list_title)
            val btnEditList: ImageView = view.findViewById(R.id.btn_edit_list)
            val rvTasks: RecyclerView = view.findViewById(R.id.rv_tasks)
            val completedSection: View = view.findViewById(R.id.completed_section)
            val completedHeader: View = view.findViewById(R.id.completed_header)
            val tvCompletedCount: TextView = view.findViewById(R.id.tv_completed_count)
            val tvClearCompleted: TextView = view.findViewById(R.id.tv_clear_completed)
            val ivCompletedToggle: ImageView = view.findViewById(R.id.iv_completed_toggle)
            val cardCompleted: View = view.findViewById(R.id.card_completed)
            val rvCompleted: RecyclerView = view.findViewById(R.id.rv_completed_tasks)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListPageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.page_task_list, parent, false)
            return ListPageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ListPageViewHolder, position: Int) {
            val listKey = taskLists.keys.toList()[position]
            val listState = taskLists[listKey] ?: TaskListState()
            val activeTasks = listState.active
            val completedTasks = listState.completed

            holder.tvListTitle.text = listKey

            lateinit var activeAdapter: TaskAdapter
            lateinit var completedAdapter: TaskAdapter

            activeAdapter = TaskAdapter(activeTasks) { taskPos ->
                if (taskPos in activeTasks.indices) {
                    val completedTask = activeTasks.removeAt(taskPos)
                    completedTask.isCompleted = true
                    completedTasks.add(0, completedTask)
                    activeAdapter.notifyItemRemoved(taskPos)
                    completedAdapter.notifyItemInserted(0)
                    updateCompletedSection(holder, listState)
                }
            }

            completedAdapter = TaskAdapter(completedTasks) { taskPos ->
                if (taskPos in completedTasks.indices) {
                    val restoredTask = completedTasks.removeAt(taskPos)
                    restoredTask.isCompleted = false
                    activeTasks.add(0, restoredTask)
                    completedAdapter.notifyItemRemoved(taskPos)
                    activeAdapter.notifyItemInserted(0)
                    holder.rvTasks.scrollToPosition(0)
                    updateCompletedSection(holder, listState)
                }
            }

            holder.rvTasks.layoutManager = LinearLayoutManager(context)
            holder.rvTasks.adapter = activeAdapter

            holder.rvCompleted.layoutManager = LinearLayoutManager(context)
            holder.rvCompleted.adapter = completedAdapter

            holder.completedHeader.setOnClickListener {
                listState.isCompletedExpanded = !listState.isCompletedExpanded
                updateCompletedSection(holder, listState)
            }

            holder.ivCompletedToggle.setOnClickListener {
                listState.isCompletedExpanded = !listState.isCompletedExpanded
                updateCompletedSection(holder, listState)
            }

            holder.tvClearCompleted.setOnClickListener {
                if (completedTasks.isNotEmpty()) {
                    showDeleteCompletedDialog {
                        completedTasks.clear()
                        completedAdapter.notifyDataSetChanged()
                        updateCompletedSection(holder, listState)
                    }
                }
            }

            updateCompletedSection(holder, listState)

            holder.btnEditList.setOnClickListener {
                showListDialog("Rename List", listKey) { newName ->
                    if (newName.isNotBlank() && newName != listKey && !taskLists.containsKey(newName)) {
                        val listState = taskLists.remove(listKey) ?: TaskListState()

                        val newMap = LinkedHashMap<String, TaskListState>()
                        taskLists.forEach { (k, v) -> newMap[k] = v }
                        newMap[newName] = listState

                        taskLists.clear()
                        taskLists.putAll(newMap)
                        notifyDataSetChanged()
                    }
                }
            }
        }

        override fun getItemCount() = taskLists.size

        private fun updateCompletedSection(holder: ListPageViewHolder, listState: TaskListState) {
            val completedCount = listState.completed.size
            val hasCompleted = completedCount > 0

            holder.completedSection.visibility = if (hasCompleted) View.VISIBLE else View.GONE
            holder.tvCompletedCount.text = getString(R.string.completed_count, completedCount)
            holder.tvClearCompleted.visibility = if (hasCompleted) View.VISIBLE else View.GONE
            holder.cardCompleted.visibility = if (listState.isCompletedExpanded) View.VISIBLE else View.GONE
            holder.ivCompletedToggle.rotation = if (listState.isCompletedExpanded) 0f else -90f
        }
    }
}

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onToggleComplete: (Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_task_title)
        val ivCheckbox: ImageView = view.findViewById(R.id.iv_checkbox)
        val ivStar: ImageView = view.findViewById(R.id.iv_star)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.title.text = task.title

        val colorPrimary = MaterialColors.getColor(holder.itemView, androidx.appcompat.R.attr.colorPrimary)
        val colorOnSurface = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurface)
        val colorOnSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)

        if (task.isCompleted) {
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.title.setTextColor(colorOnSurfaceVariant)
            holder.ivCheckbox.setImageResource(R.drawable.ic_check_circle_themed)
            holder.ivCheckbox.setColorFilter(colorOnSurfaceVariant)
            holder.ivStar.alpha = 0.5f
        } else {
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.title.setTextColor(colorOnSurface)
            holder.ivCheckbox.setImageResource(R.drawable.ic_circle_outline)
            holder.ivCheckbox.setColorFilter(colorPrimary)
            holder.ivStar.alpha = 1f
        }

        // Update star icon based on state
        if (task.isStarred) {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            holder.ivStar.setImageResource(R.drawable.ic_star_outline)
        }

        holder.ivCheckbox.setOnClickListener {
            onToggleComplete(holder.adapterPosition)
        }

        holder.ivStar.setOnClickListener {
            task.isStarred = !task.isStarred
            notifyItemChanged(holder.adapterPosition)
        }

        holder.itemView.setOnClickListener {
            if (task.isCompleted) {
                onToggleComplete(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount() = tasks.size
}