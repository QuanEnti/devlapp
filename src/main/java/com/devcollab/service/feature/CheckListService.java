package com.devcollab.service.feature;

import com.devcollab.dto.CheckListDTO;
import com.devcollab.domain.User;
import java.util.List;

public interface CheckListService {

    List<CheckListDTO> getByTask(Long taskId);

    CheckListDTO addItem(Long taskId, String item, User actor);

    CheckListDTO toggleItem(Long id, boolean done, User actor);

    void deleteItem(Long id, User actor);
}
