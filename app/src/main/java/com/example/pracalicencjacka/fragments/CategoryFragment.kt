package com.example.pracalicencjacka.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.adapters.CategoryAdapter
import com.example.arxivWebFeed.databinding.FragmentCategoryBinding
import com.example.pracalicencjacka.Utils
import com.example.pracalicencjacka.viewmodel.MainViewModel

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    internal val viewModel: MainViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    lateinit var recyclerview: RecyclerView
    private var categoryImages: ArrayList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        categoryImages.add(R.drawable.physics)
        categoryImages.add(R.drawable.mathematics)
        categoryImages.add(R.drawable.computerscience)
        categoryImages.add(R.drawable.quantitativebiology)
        categoryImages.add(R.drawable.quantitativefinance)
        categoryImages.add(R.drawable.statistics)
        categoryImages.add(R.drawable.eeass)
        categoryImages.add(R.drawable.economics)
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        val view = binding.root
        recyclerview = binding.listViewField
        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        categoryAdapter = CategoryAdapter(viewModel.categories.value, categoryImages)
        val mDividerItemDecoration = DividerItemDecoration(
            recyclerview.context,
            (recyclerview.layoutManager as LinearLayoutManager).orientation
        )
        recyclerview.addItemDecoration(mDividerItemDecoration)

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = categoryAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.updateAdapter(viewModel.categories.value!!)
        }
        categoryAdapter.setOnItemClickListener(object : CategoryAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                viewModel.queryAndSet()
                viewModel._currentCategory.value = viewModel.categories.value?.get(position)
                viewModel._currentCategoryPosition.value = position
                findNavController().navigate(R.id.action_chooseFieldFragment_to_subFieldFragment)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menumain, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.subcategories -> findNavController().navigate(R.id.action_chooseFieldFragment_to_subscribedFieldsFragment)
            R.id.settings -> Utils().showAlertDialog(requireContext())
        }
        return true
    }
}



