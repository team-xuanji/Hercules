package team.magic.flute.hercules.manager.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.util.Tuple2;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;
import team.magic.flute.hercules.manager.vo.ExecutorInfoReportRequestVO;

import java.util.Collection;

@RestController
@RequestMapping("/executorManager")
@Slf4j
public class ExecutorManagerController {
    @Autowired
    private HerculesExecutorInfoService herculesExecutorInfoService;

    @PostMapping("/reportExecutorInfo")
    public BaseResponse<Boolean> reportExecutorInfo(@RequestBody ExecutorInfoReportRequestVO requestVO){
        return BaseResponse.success(herculesExecutorInfoService.saveOrUpdate(requestVO.parse2Po()));
    }

    @GetMapping("/listAllRunningExecutor")
    public BaseResponse<Collection<HerculesExecutorInfo>> getAllExecutorInfo(){
        return BaseResponse.success(herculesExecutorInfoService.getAllExecutorInfo());
    }
}
