package com.semtrack.evaluations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.semtrack.R
import com.semtrack.data.local.EvaluationCategoryEntity
import com.semtrack.data.local.EvaluationItemEntity
import kotlinx.coroutines.launch
import java.util.Locale

sealed class EvalListItem {
    data class CategoryHeader(
        val category: EvaluationCategoryEntity,
        val obtainedPercentage: Double
    ) : EvalListItem()
    
    data class ItemRow(
        val item: EvaluationItemEntity, 
        val itemWeightage: Double,
        val obtainedWeightage: Double?,
        val isDropped: Boolean,
        val isWeightageNull: Boolean
    ) : EvalListItem()
}

class EvaluationDetailFragment : Fragment() {

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_COURSE_NAME = "course_name"

        fun newInstance(courseId: Long, courseName: String): EvaluationDetailFragment {
            return EvaluationDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COURSE_ID, courseId)
                    putString(ARG_COURSE_NAME, courseName)
                }
            }
        }
    }

    private var courseId: Long = 0
    private var courseName: String = ""

    private val viewModel: EvaluationDetailViewModel by viewModels {
        EvaluationDetailViewModel.Factory(requireContext().applicationContext, courseId)
    }

    private lateinit var tvCourseName: TextView
    private lateinit var tvTotalObtained: TextView
    private lateinit var tvTotalEvaluated: TextView
    private lateinit var rvEvaluations: RecyclerView
    private lateinit var fabAddCategory: FloatingActionButton

    private lateinit var adapter: EvaluationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseId = it.getLong(ARG_COURSE_ID)
            courseName = it.getString(ARG_COURSE_NAME).orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_evaluation_detail, container, false)

        tvCourseName = view.findViewById(R.id.tv_course_name)
        tvTotalObtained = view.findViewById(R.id.tv_total_obtained)
        tvTotalEvaluated = view.findViewById(R.id.tv_total_evaluated)
        rvEvaluations = view.findViewById(R.id.rv_evaluations)
        fabAddCategory = view.findViewById(R.id.fab_add_evaluation)

        tvCourseName.text = courseName

        adapter = EvaluationAdapter(
            onItemClick = { itemRow -> showItemMarksDialog(itemRow.item) },
            onCategoryLongClick = { category -> showCategoryOptionsDialog(category) }
        )
        rvEvaluations.layoutManager = LinearLayoutManager(requireContext())
        rvEvaluations.adapter = adapter

        fabAddCategory.setOnClickListener {
            showCategoryDialog()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    tvTotalObtained.text = String.format(Locale.getDefault(), "%.2f%%", state.totalObtainedPercentage)
                    tvTotalEvaluated.text = String.format(Locale.getDefault(), "%.2f%%", state.totalEvaluatedWeightage)
                    
                    val listItems = mutableListOf<EvalListItem>()
                    for (catWithItems in state.categories) {
                        val category = catWithItems.category
                        
                        val consideredCount = category.bestOf ?: category.itemCount
                        val isWeightageNull = category.weightage == null
                        val itemWeightage = if (consideredCount > 0 && !isWeightageNull) category.weightage / consideredCount else 0.0
                        
                        // First calculate the items to figure out what's dropped and what's kept
                        val evaluatedItems = catWithItems.items.filter { it.totalMarks != null && it.totalMarks > 0 }
                            .map { item ->
                                val obt = item.marksObtained ?: 0.0
                                val max = item.totalMarks ?: 1.0
                                Pair(item.id, (obt / max) * itemWeightage)
                            }
                            .sortedByDescending { it.second }
                        
                        val topItems = evaluatedItems.take(consideredCount)
                        val topItemIds = topItems.map { it.first }.toSet()
                        
                        val categoryObtained = topItems.sumOf { it.second }
                        
                        listItems.add(EvalListItem.CategoryHeader(category, categoryObtained))
                        
                        for (item in catWithItems.items) {
                            val isEvaluated = item.totalMarks != null && item.totalMarks > 0
                            val obtainedWeightage = if (isEvaluated) {
                                val obt = item.marksObtained ?: 0.0
                                val max = item.totalMarks ?: 1.0
                                (obt / max) * itemWeightage
                            } else null
                            
                            val isDropped = isEvaluated && !topItemIds.contains(item.id)
                            
                            listItems.add(EvalListItem.ItemRow(item, itemWeightage, obtainedWeightage, isDropped, isWeightageNull))
                        }
                    }
                    adapter.submitList(listItems)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Evaluation Details"
    }

    private fun showCategoryDialog(category: EvaluationCategoryEntity? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_evaluation_category, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_cat_name)
        val etWeightage = dialogView.findViewById<TextInputEditText>(R.id.et_cat_weightage)
        val etItemCount = dialogView.findViewById<TextInputEditText>(R.id.et_cat_item_count)
        val etBestOf = dialogView.findViewById<TextInputEditText>(R.id.et_cat_best_of)

        val isEdit = category != null

        if (isEdit) {
            etName.setText(category!!.name)
            etWeightage.setText(category.weightage?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
            etItemCount.setText(category.itemCount.toString())
            etBestOf.setText(category.bestOf?.toString() ?: "")
        }

        val title = if (isEdit) "Edit Category" else "Add Assessment Category"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etName.text.toString().trim()
                val weightStr = etWeightage.text.toString().trim()
                val countStr = etItemCount.text.toString().trim()
                val bestOfStr = etBestOf.text.toString().trim()

                if (name.isEmpty() || countStr.isEmpty()) {
                    if (name.isEmpty()) etName.error = "Required"
                    if (countStr.isEmpty()) etItemCount.error = "Required"
                    return@setOnClickListener
                }

                val currentCategories = viewModel.uiState.value.categories
                val nameExists = currentCategories.any { 
                    it.category.name.equals(name, ignoreCase = true) && 
                    (!isEdit || it.category.id != category?.id) 
                }
                if (nameExists) {
                    etName.error = "Category name already exists"
                    return@setOnClickListener
                }

                val weight = weightStr.toDoubleOrNull()
                val count = countStr.toIntOrNull() ?: 0
                val bestOf = bestOfStr.toIntOrNull()

                if (weight != null && (weight <= 0 || weight > 100)) {
                    etWeightage.error = "Must be between 0 and 100"
                    return@setOnClickListener
                }
                
                if (count <= 0) {
                    etItemCount.error = "Must be > 0"
                    return@setOnClickListener
                }
                
                if (bestOf != null && (bestOf <= 0 || bestOf > count)) {
                    etBestOf.error = "Must be between 1 and $count"
                    return@setOnClickListener
                }


                val currentWeightSum = currentCategories
                    .filter { !isEdit || it.category.id != category?.id }
                    .sumOf { it.category.weightage ?: 0.0 }
                if (weight != null && currentWeightSum + weight > 100) {
                    etWeightage.error = "Total course weightage exceeds 100% (currently ${currentWeightSum}%)"
                    return@setOnClickListener
                }

                if (isEdit) {
                    viewModel.editCategory(category!!.id, name, weight, count, bestOf)
                } else {
                    viewModel.addCategory(name, weight, count, bestOf)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showItemMarksDialog(item: EvaluationItemEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_evaluation_item_marks, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_item_title)
        val etObtained = dialogView.findViewById<TextInputEditText>(R.id.et_item_marks_obtained)
        val etTotal = dialogView.findViewById<TextInputEditText>(R.id.et_item_total_marks)

        tvTitle.text = "Enter Marks for ${item.name}"
        etObtained.setText(item.marksObtained?.toString() ?: "")
        etTotal.setText(item.totalMarks?.toString() ?: "")

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val obtainedStr = etObtained.text.toString().trim()
                val totalStr = etTotal.text.toString().trim()

                val obtained = obtainedStr.toDoubleOrNull()
                val total = totalStr.toDoubleOrNull()

                if (total != null && total <= 0) {
                    etTotal.error = "Must be > 0"
                    return@setOnClickListener
                }

                if (obtained != null && total == null) {
                    etTotal.error = "Required if obtained marks entered"
                    return@setOnClickListener
                }

                if (obtained != null && total != null && obtained > total) {
                    etObtained.error = "Cannot exceed total marks"
                    return@setOnClickListener
                }

                viewModel.updateItemMarks(item.id, item.categoryId, item.name, total, obtained)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showCategoryOptionsDialog(category: EvaluationCategoryEntity) {
        val options = arrayOf("Edit Category", "Delete Category")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(category.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCategoryDialog(category)
                    1 -> showDeleteCategoryDialog(category)
                }
            }
            .show()
    }

    private fun showDeleteCategoryDialog(category: EvaluationCategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}' and all its items?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(category.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private class EvaluationAdapter(
        private val onItemClick: (EvalListItem.ItemRow) -> Unit,
        private val onCategoryLongClick: (EvaluationCategoryEntity) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<EvalListItem> = emptyList()

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is EvalListItem.CategoryHeader -> 0
                is EvalListItem.ItemRow -> 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                CategoryViewHolder(inflater.inflate(R.layout.item_evaluation_category, parent, false))
            } else {
                ItemViewHolder(inflater.inflate(R.layout.item_evaluation_entry, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is EvalListItem.CategoryHeader -> {
                    val catHolder = holder as CategoryViewHolder
                    catHolder.bind(item)
                    catHolder.itemView.setOnLongClickListener {
                        onCategoryLongClick(item.category)
                        true
                    }
                }
                is EvalListItem.ItemRow -> {
                    val itemHolder = holder as ItemViewHolder
                    itemHolder.bind(item)
                    itemHolder.itemView.setOnClickListener { onItemClick(item) }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun submitList(newList: List<EvalListItem>) {
            items = newList
            notifyDataSetChanged()
        }

        class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_category_name)
            val tvObtained: TextView = view.findViewById(R.id.tv_category_obtained)
            val tvWeightage: TextView = view.findViewById(R.id.tv_category_weightage)
            val tvRule: TextView = view.findViewById(R.id.tv_category_rule)

            fun bind(header: EvalListItem.CategoryHeader) {
                val category = header.category
                tvName.text = category.name
                
                val obt = String.format(Locale.getDefault(), "%.2f%%", header.obtainedPercentage)
                tvObtained.text = obt
                if (category.weightage != null) {
                    val total = String.format(Locale.getDefault(), "%.2f%%", category.weightage)
                    tvWeightage.text = " / $total"
                } else {
                    tvWeightage.text = " (TBD)"
                }
                
                val ruleText = if (category.bestOf != null) "Best ${category.bestOf} of ${category.itemCount}" else "${category.itemCount} Items"
                tvRule.text = ruleText
            }
        }

        class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_eval_name)
            val tvContribution: TextView = view.findViewById(R.id.tv_eval_contribution)
            val tvMarks: TextView = view.findViewById(R.id.tv_eval_marks)

            fun bind(row: EvalListItem.ItemRow) {
                tvName.text = row.item.name
                val w = if (row.isWeightageNull) "TBD" else String.format(Locale.getDefault(), "%.2f", row.itemWeightage)
                
                if (row.item.totalMarks != null && row.item.totalMarks > 0) {
                    val obtMarks = row.item.marksObtained ?: 0.0
                    tvMarks.text = "Marks: $obtMarks / ${row.item.totalMarks}"
                    
                    if (row.isDropped) {
                        val obtW = String.format(Locale.getDefault(), "%.2f", row.obtainedWeightage ?: 0.0)
                        tvContribution.text = "+ $obtW% (Dropped)"
                        tvContribution.alpha = 0.5f
                        tvName.paintFlags = tvName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        itemView.alpha = 0.6f
                    } else {
                        val obtW = String.format(Locale.getDefault(), "%.2f", row.obtainedWeightage ?: 0.0)
                        tvContribution.text = "+ $obtW%"
                        tvContribution.alpha = 1.0f
                        tvName.paintFlags = tvName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        itemView.alpha = 1.0f
                    }
                } else {
                    tvMarks.text = "Marks: Not Evaluated"
                    if (row.isWeightageNull) {
                        tvContribution.text = "Max: TBD"
                    } else {
                        tvContribution.text = "Max: $w%"
                    }
                    tvContribution.alpha = 0.5f
                    tvName.paintFlags = tvName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    itemView.alpha = 1.0f
                }
            }
        }
    }
}
