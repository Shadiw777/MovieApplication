package com.example.movieapplication.ui.movies

import androidx.lifecycle.*
import com.example.movieapplication.data.network.MovieRepository
import com.example.movieapplication.data.network.model.Movie
import com.example.movieapplication.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class MovieViewModel @Inject constructor(private val repository: MovieRepository) :
    ViewModel() {

    private var debouncePeriod: Long = 500
    private var searchJob: Job? = null
    private var _searchMoviesLiveData: LiveData<List<Movie>>
    private val _searchFieldTextLiveData = MutableLiveData<String>()
    private val _popularMoviesLiveData = MutableLiveData<List<Movie>>()
    private val _navigateToDetails = MutableLiveData<Event<String>>()
    val navigateToDetails: LiveData<Event<String>>
        get() = _navigateToDetails

    val moviesMediatorData = MediatorLiveData<List<Movie>>()
    val movieLoadingStateLiveData = MutableLiveData<MovieLoadingState>()

    init {
        _searchMoviesLiveData = Transformations.switchMap(_searchFieldTextLiveData) {
            fetchMovieByQuery(it)
        }

        moviesMediatorData.addSource(_popularMoviesLiveData) {
            moviesMediatorData.value = it
        }

        moviesMediatorData.addSource(_searchMoviesLiveData) {
            moviesMediatorData.value = it
        }
    }

    fun onFragmentReady() {
        if (_popularMoviesLiveData.value.isNullOrEmpty()) {
            fetchPopularMovies()
        }
    }

    fun onSearchQuery(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(debouncePeriod)

            if (query.length > 2) {
                _searchFieldTextLiveData.value = query
            }
        }
    }

    private fun fetchPopularMovies() {
        movieLoadingStateLiveData.value = MovieLoadingState.LOADING

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val movies = repository.fetchPopularMovies()
                _popularMoviesLiveData.postValue(movies!!)

                movieLoadingStateLiveData.postValue(MovieLoadingState.LOADED)
            } catch (e: Exception) {
                movieLoadingStateLiveData.postValue(MovieLoadingState.INVALID_API_KEY)
            }
        }
    }

    private fun fetchMovieByQuery(query: String): LiveData<List<Movie>> {
        val liveData = MutableLiveData<List<Movie>>()
        viewModelScope.launch(Dispatchers.IO) {
            val movies = repository.fetchMovieByQuery(query)
            liveData.postValue(movies!!)
        }

        return liveData
    }

    fun onMovieClicked(movie: Movie) {
        movie.title?.let{
            _navigateToDetails.value = Event(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

}