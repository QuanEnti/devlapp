package com.devcollab.service.feature;

import com.devcollab.domain.CheckList;
import java.util.List;

public interface CheckListService {

    List<CheckList> getChecklistByTask(Long taskId);

    CheckList createChecklistItem(Long taskId, String itemText);

    CheckList toggleChecklistItem(Long checklistId, boolean isDone);

    CheckList updateChecklistItem(Long checklistId, String newText);

    void deleteChecklistItem(Long checklistId);
}
