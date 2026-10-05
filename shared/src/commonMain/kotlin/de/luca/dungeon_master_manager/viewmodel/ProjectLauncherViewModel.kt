package de.luca.dungeon_master_manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.luca.dungeon_master_manager.data.ProjectInfo
import de.luca.dungeon_master_manager.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectLauncherViewModel : ViewModel() {
    private val repository = ProjectRepository()

    private val _projects = MutableStateFlow<List<ProjectInfo>>(emptyList())
    val projects: StateFlow<List<ProjectInfo>> = _projects

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _projects.value = repository.listProjects()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            try {
                repository.createProject(name)
                loadProjects()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteProject(project: ProjectInfo) {
        viewModelScope.launch {
            try {
                repository.deleteProject(project)
                loadProjects()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
