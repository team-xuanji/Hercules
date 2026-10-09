package team.magic.flute.hercules.manager.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;
import team.magic.flute.hercules.manager.vo.ExecutorInfoReportRequestVO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/executorManager")
@Slf4j
public class ExecutorManagerController {
    @Autowired
    private HerculesExecutorInfoService herculesExecutorInfoService;
    @Autowired
    private RunnerEnv runnerEnv;

    @PostMapping("/reportExecutorInfo")
    public BaseResponse<Boolean> reportExecutorInfo(@Valid @RequestBody ExecutorInfoReportRequestVO requestVO){
        return BaseResponse.success(herculesExecutorInfoService.saveOrUpdate(requestVO.parse2Po(runnerEnv.getHttpEncryptKey())));
    }

    @GetMapping("/listAllRunningExecutor")
    public BaseResponse<Collection<HerculesExecutorInfo>> getAllExecutorInfo(){
        return BaseResponse.success(herculesExecutorInfoService.getAllExecutorInfo()
                .stream().peek(x-> x.setIdentityId(null)).collect(Collectors.toList()));
    }
}
