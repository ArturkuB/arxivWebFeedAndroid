package com.example.pracalicencjacka.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.arxivWebFeed.BuildConfig
import com.example.pracalicencjacka.adapters.PublicationAdapter
import com.example.arxivWebFeed.databinding.FragmentPublicationsBinding
import com.example.pracalicencjacka.interfaces.AsyncResponse
import com.example.pracalicencjacka.network.DownloadFile
import com.example.pracalicencjacka.viewmodel.MainViewModel
import io.realm.toRealmList
import java.io.File

class PublicationFragment : Fragment(), AsyncResponse {
    private var _binding: FragmentPublicationsBinding? = null
    private val binding get() = _binding!!

    internal val viewModel: MainViewModel by activityViewModels()
    private lateinit var publicationAdapter: PublicationAdapter
    private var pdfTitle: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublicationsBinding.inflate(inflater, container, false)
        val view = binding.root

        val recyclerview = binding.recycleViewPubs
        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        publicationAdapter = if (viewModel.subscribedCurrentSubcategory) {
            val rowsSubscribed = viewModel.currentSubcategory.value!!.publications.toRealmList()
            PublicationAdapter(rowsSubscribed)
        } else {
            val rowsUnsubscribed = viewModel.publicationsArray?.toRealmList()
            PublicationAdapter(rowsUnsubscribed)
        }

        val mDividerItemDecoration = DividerItemDecoration(
            recyclerview.context,
            (recyclerview.layoutManager as LinearLayoutManager).orientation
        )
        recyclerview.addItemDecoration(mDividerItemDecoration)

        recyclerview.adapter = publicationAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        publicationAdapter.setOnItemClickListener(object : PublicationAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                viewModel._currentPublicationPosition.value = position
                val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                builder.setTitle("Choose a way of displaying PDF")
                builder.setPositiveButton("Download/Open") { _, _ ->
                    downloadPDF(position)
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.show()
            }
        })

        viewModel.currentSubcategory.observe(viewLifecycleOwner) {
            if (viewModel.subscribedCurrentSubcategory) {
                publicationAdapter.updateAdapter(viewModel.currentSubcategory.value!!.publications.toRealmList())
            }
        }
    }

    fun downloadPDF(position: Int) {
        val folder: File
        var file = viewModel.checkIfPdfExists(requireContext())
        if (!file.exists()) {
            val pdfLink: String
            if (viewModel.subscribedCurrentSubcategory) {
                pdfTitle = viewModel.currentSubcategory.value!!.publications[position].title
                pdfLink =
                    "https://arxiv.org" + viewModel.currentSubcategory.value!!.publications[position].link
            } else {
                pdfTitle = viewModel.publicationsArray!![position].title
                pdfLink = "https://arxiv.org" + viewModel.publicationsArray!![position].link
            }
            val download = DownloadFile(requireContext())
            download.execute(pdfLink, "$pdfTitle.pdf")
            download.delegate = this
            folder = viewModel.managePdfDirectory(requireContext())
            val path = "$folder/$pdfTitle.pdf"
            file = File(path)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(
                FileProvider.getUriForFile(
                    requireContext(),
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                ), "application/pdf"
            )
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }

    override fun processFinish(output: String?, folder: File) {
        if (output.equals("1")) {
            Toast.makeText(requireContext(), "PDF Downloaded", Toast.LENGTH_SHORT).show()
            val path = "$folder/$pdfTitle.pdf"
            val file = File(path)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(
                FileProvider.getUriForFile(
                    requireContext(),
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                ), "application/pdf"
            )
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)

        } else {
            Toast.makeText(requireContext(), "Download Failed!", Toast.LENGTH_SHORT).show()
        }
    }
}

