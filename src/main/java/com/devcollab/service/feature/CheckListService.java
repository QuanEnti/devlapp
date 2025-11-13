package com.devcollab.service.feature;

import com.devcollab.domain.CheckList;
import java.util.List;
import org.springframework.stereotype.Service;

public interface CheckListService {

    List<CheckList> getByTask(Long taskId);

    CheckList addItem(Long taskId, String item);

    CheckList toggleItem(Long id, boolean done);

    void deleteItem(Long id);
}
