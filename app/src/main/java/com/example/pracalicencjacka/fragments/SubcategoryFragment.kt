package com.example.pracalicencjacka.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.Menu
import com.example.pracalicencjacka.viewmodel.MainViewModel
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.adapters.SubcategoryAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.arxivWebFeed.databinding.FragmentSubcategoryBinding
import com.example.pracalicencjacka.Utils
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class SubcategoryFragment : Fragment() {
    private var _binding: FragmentSubcategoryBinding? = null
    private val binding get() = _binding!!
    internal val viewModel: MainViewModel by activityViewModels()
    lateinit var subcategoryAdapter: SubcategoryAdapter
    lateinit var recyclerview: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menumain, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.subcategories -> findNavController().navigate(R.id.action_subFieldFragment_to_subscribedFieldsFragment)
            R.id.categories -> findNavController().navigate(R.id.action_subFieldFragment_to_chooseFieldFragment)
            R.id.settings -> Utils().showAlertDialog(requireContext())
        }
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubcategoryBinding.inflate(inflater, container, false)
        val view = binding.root
        recyclerview = binding.recycleViewSubcategory
        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        subcategoryAdapter = SubcategoryAdapter(requireContext(), viewModel.currentCategory.value)

        val mDividerItemDecoration = DividerItemDecoration(
            recyclerview.context,
            (recyclerview.layoutManager as LinearLayoutManager).orientation
        )
        recyclerview.addItemDecoration(mDividerItemDecoration)
        recyclerview.adapter = subcategoryAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadingState.observe(viewLifecycleOwner) {
            if (it == false) {
                findNavController().navigate(R.id.action_subFieldFragment_to_publicationsFragment)
            }
            binding.progressBar.isVisible = it
            binding.recycleViewSubcategory.isVisible = !it
        }
        subcategoryAdapter.setOnItemClickListener(object : SubcategoryAdapter.OnItemClickListener {
            override fun cellClick(position: Int) {
                viewModel._currentSubcategoryPosition.value = position
                viewModel._currentSubcategory.value =
                    viewModel._currentCategory.value!!.subcategories[position]
                viewModel.manageSubcategoryCellClick()
            }

            override fun starClick(position: Int, checked: Boolean) {
                if (checked) {
                    viewModel._currentSubcategoryPosition.value = position
                    viewModel._currentSubcategory.value =
                        viewModel._currentCategory.value!!.subcategories[position]
                    lifecycleScope.launch(Main) {
                        viewModel.subscribeItem(position) {
                            subcategoryAdapter.updateAdapter(viewModel.currentCategory.value)
                        }
                    }
                } else {
                    viewModel.setCurrentSubcategory(position)
                    viewModel.unsubscribeItem(position) {
                        subcategoryAdapter.updateAdapter(viewModel.currentCategory.value)
                    }
                }
            }
        })
    }
}