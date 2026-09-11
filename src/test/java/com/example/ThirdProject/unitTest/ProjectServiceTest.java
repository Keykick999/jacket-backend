package com.example.ThirdProject.unitTest;

import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.project.dto.ProjectDto;
import com.example.ThirdProject.project.dto.TrackDto;
import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.enums.TrackType;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.project.service.ProjectService;
import com.example.ThirdProject.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    // 프로젝트 생성 테스트
    @Test
    void createProject_shouldCreateProjectWithTracks() {
        // given
        User user = createUser("user1");

        TrackDto track1 = createTrackDto(1);
        TrackDto track2 = createTrackDto(2);

        ProjectDto projectDto = ProjectDto.builder()
                .name("test project")
                .bpm(120)
                .timeSignature("4/4")
                .tracks(List.of(track1, track2))
                .build();

        given(projectRepository.save(any(Project.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProjectDto result = projectService.createProject(user, projectDto);

        // then
        assertThat(result.getTracks()).hasSize(2);
        assertThat(result.getName()).isEqualTo("test project");
        assertThat(result.getTracks().get(0).getName()).isEqualTo("track-1");
    }

    @Test
    void createProject_shouldWorkWhenTracksIsNull() {
        // given
        User user = createUser("user1");

        ProjectDto projectDto = new ProjectDto();
        projectDto.setName("testProject");
        projectDto.setBpm(120);
        projectDto.setTimeSignature("4/4");
        projectDto.setTracks(null);

        given(projectRepository.save(any(Project.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProjectDto result = projectService.createProject(user, projectDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("testProject");
    }

    // 프로젝트 수정 테스트
    @Test
    public void updateProject_shouldNotWorkWhenProjectIsAbsent() {
        // 프로젝트 수정에 필요한 매개변수
        Long projectId = 1L;
        String userId = "id";
        ProjectDto projectDto = createProjectDto(1L, 1L);

        // projectId에 해당하는 프로젝트가 없는 상황
        given(projectRepository.findById(projectId))
                .willReturn(Optional.empty());

        // 결과는 예외가 발생해야
        assertThatThrownBy(() ->
                projectService.updateProject(projectId, userId, projectDto))
         .isInstanceOf(CustomException.class)
         .extracting("errorCode")
         .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test
    public void updateProject_shouldNotWorkWhenUserIsNotOwner() {
        // 프로젝트 수정에 필요한 매개변수
        Long projectId = 1L;
        String userId = "id";
        User user = createUser(userId);
        ProjectDto projectDto = createProjectDto(1L, 1L);
        Project project = new Project();
        project.setId(projectId);
        project.setUser(user);

        // project 제대로 반환
        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // 결과는 예외가 발생해야
        assertThatThrownBy(() ->
                projectService.updateProject(projectId, "wrongUserId", projectDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    public void updateProject_shouldWork() {
        // 프로젝트 수정에 필요한 매개변수
        Long projectId = 1L;
        String userId = "id";
        User user = createUser(userId);

        ProjectDto projectDto = createProjectDto(1L, 1L);
        TrackDto trackDto1 = createTrackDto(1);
        TrackDto trackDto2 = createTrackDto(2);
        projectDto.setTracks(List.of(trackDto1, trackDto2));
        projectDto.setName("new-name");

        Project project = new Project();
        project.setId(projectId);
        project.setUser(user);
        project.setTracks(new ArrayList<>());
        project.setName("original-project-ame");

        // when
        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));
        given(projectRepository.save(any(Project.class)))
                .willReturn(project);

        ProjectDto result = projectService.updateProject(projectId, userId, projectDto);

        // 문제 없이 수정됐어야
        assertThat(result.getTracks()).hasSize(2);
        assertThat(result.getName()).isEqualTo("new-name");
    }

    @Test
    void deleteProject_shouldNotWorkWhenUserIsNotOwner() {
        // given
        Long projectId = 1L;
        String userId = "user1";

        User owner = createUser("owner");

        Project project = new Project();
        project.setId(projectId);
        project.setUser(owner);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() ->
                projectService.deleteProject(userId, projectId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void deleteProject_shouldWork() {
        // given
        Long projectId = 1L;
        String userId = "user1";

        User user = createUser(userId);

        Project project = new Project();
        project.setId(projectId);
        project.setUser(user);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // when
        projectService.deleteProject(userId, projectId);

        // then
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_shouldNotWorkWhenProjectIsAbsent() {
        // given
        Long projectId = 1L;
        String userId = "user1";

        given(projectRepository.findById(projectId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                projectService.deleteProject(userId, projectId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }




    // ---------------- fixture ----------------

    private User createUser(String id) {
        return new User(
                id,
                "loginId",
                "nickname",
                "password",
                "email",
                "phone",
                "name",
                "intro",
                "profile",
                false,
                LocalDateTime.now()
        );
    }

    private TrackDto createTrackDto(int orderIndex) {
        return TrackDto.builder()
                .id((long) orderIndex)
                .name("track-" + orderIndex)
                .type(TrackType.AUDIO)
                .color("#FF0000")
                .volume(0.8f)
                .pan(0.0f)
                .isMuted(false)
                .isSolo(false)
                .orderIndex(orderIndex)
                .plugins(List.of())
                .clips(List.of())
                .build();
    }

    private ProjectDto createProjectDto(Long projectId, Long albumId) {
        return ProjectDto.builder()
                .id(projectId)
                .name("project-" + projectId)
                .bpm(120)
                .timeSignature("4/4")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .albumId(albumId)
                .tracks(null)
                .build();
    }
}