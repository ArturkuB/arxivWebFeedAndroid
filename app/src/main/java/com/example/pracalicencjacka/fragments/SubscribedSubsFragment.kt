package com.example.pracalicencjacka.fragments

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.pracalicencjacka.viewmodel.MainViewModel
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.datamodel.Subcategory
import com.example.arxivWebFeed.databinding.FragmentSubscribedSubBinding
import com.example.pracalicencjacka.Utils
import com.example.pracalicencjacka.adapters.SubscribedFieldsAdapter
import io.realm.RealmList
import io.realm.toRealmList

class SubscribedSubsFragment : Fragment() {
    private var _binding: FragmentSubscribedSubBinding? = null
    private val binding get() = _binding!!
    internal val viewModel: MainViewModel by activityViewModels()
    private lateinit var subcategoryAdapter: SubscribedFieldsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscribedSubBinding.inflate(inflater, container, false)
        val view = binding.root

        val recyclerview = binding.recycleViewSubcategory

        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        val rows: RealmList<Subcategory>? = null
        viewModel.querySubscribedSubcategories {
            subcategoryAdapter.updateAdapter(it.toRealmList())
        }
        subcategoryAdapter = SubscribedFieldsAdapter(requireContext(), rows)
        //subFieldAdapter.setHasStableIds(true)

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
            if (it == false)
                findNavController().navigate(R.id.action_subscribedFieldsFragment_to_publicationsFragment)
            binding.progressBar.isVisible = it
            binding.recycleViewSubcategory.isVisible = !it
        }
        subcategoryAdapter.setOnItemClickListener(object: SubscribedFieldsAdapter.OnItemClickListener{
            override fun onItemClick(subcategory: Subcategory) {
                viewModel.subscribedCurrentSubcategory = true
                viewModel.parseSubscribedSubcategory(subcategory)
            }

            override fun unsubscribeItem(subcategory: Subcategory){
                viewModel.unsubscribeSubcategory(subcategory)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menumain, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.categories -> findNavController().navigate(R.id.action_subscribedFieldsFragment_to_chooseFieldFragment)
            R.id.subcategories -> findNavController().navigate(R.id.subscribedFieldsFragment)
            R.id.settings -> Utils().showAlertDialog(requireContext())
        }
        return true
    }
}



