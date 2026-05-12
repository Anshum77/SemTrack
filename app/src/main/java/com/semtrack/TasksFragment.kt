package com.semtrack

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private val viewModel: TasksViewModel by viewModels {
        TasksViewModel.Factory(requireContext().applicationContext)
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var tabLayout: TabLayout
    private lateinit var btnAddList: ImageView

    private lateinit var pagerAdapter: ListsPagerAdapter
    private var tabMediator: TabLayoutMediator? = null
    private var currentLists: List<TaskListUiState> = emptyList()
    private var pendingSelectLastList = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        viewPager = view.findViewById(R.id.view_pager)
        fabAddTask = view.findViewById(R.id.fab_add_task)
        tabLayout = view.findViewById(R.id.tab_layout)
        btnAddList = view.findViewById(R.id.btn_add_list)

        pagerAdapter = ListsPagerAdapter(
            onRenameListRequested = { list ->
                showListDialog("Rename List", list.name) { newName ->
                    if (newName.isNotBlank() && isListNameAvailable(newName, list.id)) {
                        viewModel.renameList(list.id, newName)
                    }
                }
            },
            onToggleCompletedExpanded = { list ->
                viewModel.setCompletedExpanded(list.id, !list.isCompletedExpanded)
            },
            onClearCompleted = { list ->
                viewModel.deleteCompletedTasks(list.id)
            },
            onCompleteTask = { task ->
                viewModel.completeTask(task)
            },
            onRestoreTask = { task ->
                viewModel.restoreTask(task)
            },
            onToggleStar = { task ->
                viewModel.toggleStar(task)
            }
        )
        viewPager.adapter = pagerAdapter

        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getListName(position)
        }
        tabMediator?.attach()

        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        btnAddList.setOnClickListener {
            showListDialog("New List", "") { newName ->
                if (newName.isNotBlank() && isListNameAvailable(newName, null)) {
                    pendingSelectLastList = true
                    viewModel.addList(newName)
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { lists ->
                    currentLists = lists
                    pagerAdapter.submitLists(lists)

                    if (pendingSelectLastList && lists.isNotEmpty()) {
                        viewPager.setCurrentItem(lists.size - 1, true)
                        pendingSelectLastList = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabMediator?.detach()
        tabMediator = null
    }

    private fun showAddTaskDialog() {
        val currentList = currentLists.getOrNull(viewPager.currentItem) ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null)
        val etTaskTitle = dialogView.findViewById<EditText>(R.id.et_task_title)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val title = etTaskTitle.text.toString().trim()
                if (title.isNotBlank()) {
                    viewModel.addTask(currentList.id, title)
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

    private fun isListNameAvailable(name: String, excludeListId: Long?): Boolean {
        return currentLists.none { it.name == name && it.id != excludeListId }
    }

    // Inner adapter for ViewPager2
    inner class ListsPagerAdapter(
        private val onRenameListRequested: (TaskListUiState) -> Unit,
        private val onToggleCompletedExpanded: (TaskListUiState) -> Unit,
        private val onClearCompleted: (TaskListUiState) -> Unit,
        private val onCompleteTask: (TaskUi) -> Unit,
        private val onRestoreTask: (TaskUi) -> Unit,
        private val onToggleStar: (TaskUi) -> Unit
    ) : RecyclerView.Adapter<ListsPagerAdapter.ListPageViewHolder>() {

        private var lists: List<TaskListUiState> = emptyList()

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
            val activeAdapter: TaskAdapter
            val completedAdapter: TaskAdapter

            init {
                rvTasks.layoutManager = LinearLayoutManager(view.context)
                activeAdapter = TaskAdapter(
                    onToggleComplete = onCompleteTask,
                    onToggleStar = onToggleStar
                )
                rvTasks.adapter = activeAdapter

                rvCompleted.layoutManager = LinearLayoutManager(view.context)
                completedAdapter = TaskAdapter(
                    onToggleComplete = onRestoreTask,
                    onToggleStar = onToggleStar
                )
                rvCompleted.adapter = completedAdapter
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListPageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.page_task_list, parent, false)
            return ListPageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ListPageViewHolder, position: Int) {
            val listState = lists[position]

            holder.tvListTitle.text = listState.name
            holder.activeAdapter.submitTasks(listState.active)
            holder.completedAdapter.submitTasks(listState.completed)

            holder.completedHeader.setOnClickListener {
                onToggleCompletedExpanded(listState)
            }

            holder.ivCompletedToggle.setOnClickListener {
                onToggleCompletedExpanded(listState)
            }

            holder.tvClearCompleted.setOnClickListener {
                if (listState.completed.isNotEmpty()) {
                    showDeleteCompletedDialog {
                        onClearCompleted(listState)
                    }
                }
            }

            updateCompletedSection(holder, listState)

            holder.btnEditList.setOnClickListener {
                onRenameListRequested(listState)
            }
        }

        override fun getItemCount() = lists.size

        fun submitLists(newLists: List<TaskListUiState>) {
            lists = newLists
            notifyDataSetChanged()
        }

        fun getListName(position: Int): String {
            return lists.getOrNull(position)?.name.orEmpty()
        }

        private fun updateCompletedSection(holder: ListPageViewHolder, listState: TaskListUiState) {
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
    private val onToggleComplete: (TaskUi) -> Unit,
    private val onToggleStar: (TaskUi) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks: List<TaskUi> = emptyList()

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
            onToggleComplete(task)
        }

        holder.ivStar.setOnClickListener {
            onToggleStar(task)
        }

        holder.itemView.setOnClickListener {
            if (task.isCompleted) {
                onToggleComplete(task)
            }
        }
    }

    override fun getItemCount() = tasks.size

    fun submitTasks(newTasks: List<TaskUi>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}