package com.group4.javagrader.semester;

import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.impl.SemesterServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemesterServiceImplTest {

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Test
    void archiveUsesLockedSemesterLookupBeforeMarkingArchived() {
        Semester semester = new Semester();
        semester.setId(42L);
        semester.setArchived(false);

        when(semesterRepository.findByIdAndArchivedFalseForUpdate(42L)).thenReturn(Optional.of(semester));
        when(courseRepository.existsBySemesterIdAndArchivedFalse(42L)).thenReturn(false);
        when(assignmentRepository.existsBySemesterId(42L)).thenReturn(false);

        SemesterServiceImpl service = new SemesterServiceImpl(
                semesterRepository,
                courseRepository,
                assignmentRepository);

        service.archive(42L);

        verify(semesterRepository).findByIdAndArchivedFalseForUpdate(42L);
        verify(semesterRepository).save(semester);
    }
}
