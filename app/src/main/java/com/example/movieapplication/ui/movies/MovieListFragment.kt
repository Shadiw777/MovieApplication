/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.example.movieapplication.ui.movies

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.movieapplication.MovieApplication.Companion.application
import com.example.movieapplication.utils.connectivity.ConnectivityLiveData
import com.example.movieapplication.data.network.model.Movie
import com.raywenderlich.android.movieapp.R
import com.raywenderlich.android.movieapp.databinding.FragmentMovieListBinding
import javax.inject.Inject

class MovieListFragment : Fragment(R.layout.fragment_movie_list),
    MovieAdapter.MoviesClickListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MovieViewModel by viewModels { viewModelFactory }
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var binding: FragmentMovieListBinding
    private lateinit var connectivityLiveData: ConnectivityLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        application.appComponent.inject(this)
        connectivityLiveData = ConnectivityLiveData(application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movieAdapter = MovieAdapter(this)
        binding = FragmentMovieListBinding.bind(view)
        binding.apply {
            searchEditText.addTextChangedListener(searchTextWatcher)

            moviesRecyclerView.apply {
                adapter = movieAdapter
                hasFixedSize()
            }
        }

        initialiseObservers()
    }

    private fun initialiseObservers() {
        connectivityLiveData.observe(viewLifecycleOwner, { isAvailable ->
            when (isAvailable) {
                true -> {
                    viewModel.onFragmentReady()
                    binding.statusButton.visibility = View.GONE
                    binding.moviesRecyclerView.visibility = View.VISIBLE
                    binding.searchEditText.visibility = View.VISIBLE
                }

                false -> {
                    binding.statusButton.visibility = View.VISIBLE
                    binding.moviesRecyclerView.visibility = View.GONE
                    binding.searchEditText.visibility = View.GONE
                }
            }
        })

        viewModel.moviesMediatorData.observe(viewLifecycleOwner, {
            movieAdapter.submitList(it)
        })

        viewModel.movieLoadingStateLiveData.observe(viewLifecycleOwner, {
            onMovieLoadingStateChanged(it)
        })

        viewModel.navigateToDetails.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { movieTitle ->
                findNavController().navigate(
                    MovieListFragmentDirections.actionMovieClicked(
                        movieTitle
                    )
                )
            }
        })
    }

    override fun onMovieClicked(movie: Movie) {
        viewModel.onMovieClicked(movie)
    }

    private val searchTextWatcher = object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            // Start the search
            viewModel.onSearchQuery(editable.toString())
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }

    private fun onMovieLoadingStateChanged(state: MovieLoadingState) {
        when (state) {
            MovieLoadingState.LOADING -> {
                binding.statusButton.visibility = View.GONE
                binding.moviesRecyclerView.visibility = View.GONE
                binding.loadingProgressBar.visibility = View.VISIBLE
            }
            MovieLoadingState.LOADED -> {
                connectivityLiveData.value?.let {
                    if (it) {
                        binding.statusButton.visibility = View.GONE
                        binding.moviesRecyclerView.visibility = View.VISIBLE
                    } else {
                        binding.statusButton.visibility = View.VISIBLE
                        binding.moviesRecyclerView.visibility = View.GONE
                    }
                }

                binding.loadingProgressBar.visibility = View.GONE
            }
            MovieLoadingState.ERROR -> {
                binding.statusButton.visibility = View.VISIBLE
                context?.let {
                    binding.statusButton.setCompoundDrawables(
                        null, ContextCompat.getDrawable(it, R.drawable.no_internet), null,
                        null
                    )
                }
                binding.moviesRecyclerView.visibility = View.GONE
                binding.loadingProgressBar.visibility = View.GONE
            }
            MovieLoadingState.INVALID_API_KEY -> {
                binding.statusButton.visibility = View.VISIBLE
                binding.statusButton.text = getString(R.string.invalid_api_key)
                binding.statusButton.setCompoundDrawables(null, null, null, null)
                binding.moviesRecyclerView.visibility = View.GONE
                binding.loadingProgressBar.visibility = View.GONE
            }
        }
    }

}