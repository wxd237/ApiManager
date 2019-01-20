package cn.crap.controller.user;

import cn.crap.adapter.BugAdapter;
import cn.crap.dto.BugDTO;
import cn.crap.enu.MyError;
import cn.crap.framework.JsonResult;
import cn.crap.framework.MyException;
import cn.crap.framework.base.BaseController;
import cn.crap.framework.interceptor.AuthPassport;
import cn.crap.model.BugPO;
import cn.crap.model.Module;
import cn.crap.model.Project;
import cn.crap.query.BugQuery;
import cn.crap.service.BugService;
import cn.crap.service.MenuService;
import cn.crap.utils.MyString;
import cn.crap.utils.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/user/bug")
public class BugController extends BaseController{
    // TODO 权限目前只要项目成员即可操作

    @Autowired
    private BugService bugService;
    @Autowired
    MenuService customMenuService;

    /**
     * bug管理系统下拉选择框
     * @param request
     * @param type
     * @param def
     * @param tag
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "pick.do")
    @AuthPassport
    public String pickUp(HttpServletRequest request, String type, String def,
                          String tag) throws Exception {
        String pickContent = customMenuService.pick("true", type, null, def, "true");
        request.setAttribute("tag", tag);
        request.setAttribute("pickContent", pickContent);
        request.setAttribute("type", type);
        return "WEB-INF/views/bugPick.jsp";
    }

    /**
     * 修改缺陷状态（严重程度、优先级等）
     * @param type 待修改的状态类型
     * @param value 新的状态
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "changeBug.do")
    @ResponseBody
    @AuthPassport
    public JsonResult changeBug(String id, @RequestParam(defaultValue = "") String type, String value) throws Exception{
        // TODO 修改日志
        // id为0，忽略
        if (MyString.isEmpty(id)){
            return new JsonResult(true);
        }
        BugPO dbBug = bugService.get(id);
        checkPermission(dbBug.getProjectId(), READ);

        BugPO bug = bugService.getChangeBugPO(id, type, value);
        boolean update = bugService.update(bug);

        dbBug = bugService.get(id);
        Module module = moduleCache.get(dbBug.getModuleId());
        Project project = projectCache.get(dbBug.getProjectId());

        BugDTO dto = BugAdapter.getDto(dbBug, module, project);
        return new JsonResult(update).data(dto);
    }


    /**
     * bug列表
     * @return
     * @throws MyException
     */
    @RequestMapping("/list.do")
    @ResponseBody
    @AuthPassport
    public JsonResult list(@ModelAttribute BugQuery query) throws MyException {
        Project project = getProject(query);
        checkPermission(project, READ);

        Page page = new Page(query);
        List<BugPO> bugPOList = bugService.select(query);
        page.setAllRow(bugService.count(query));
        List<BugDTO> dtoList = BugAdapter.getDto(bugPOList);

        return new JsonResult().data(dtoList).page(page);
    }

    @RequestMapping("/detail.do")
    @ResponseBody
    @AuthPassport
    public JsonResult detail(BugDTO dto) throws MyException {
        throwExceptionWhenIsNull(dto.getProjectId(), "projectId 不能为空");
        checkPermission(dto.getProjectId(), READ);

        String id = dto.getId();
        dto.setProjectId(getProjectId(dto.getProjectId(), dto.getModuleId()));
        Project project = projectCache.get(dto.getProjectId());
        Module module = moduleCache.get(dto.getModuleId());

        if (id != null) {
            BugPO bugPO = bugService.get(id);
            module = moduleCache.get(bugPO.getModuleId());
            return new JsonResult(1, BugAdapter.getDto(bugPO, module, project));
        }

        BugDTO bugDTO = BugAdapter.getDTO(project, module);
        return new JsonResult(1, bugDTO);
    }

    @RequestMapping("/delete.do")
    @ResponseBody
    @AuthPassport
    public JsonResult delete(String id) throws MyException {
        throwExceptionWhenIsNull(id, "id");

        BugPO bugPO = bugService.get(id);
        if (bugPO == null) {
            throw new MyException(MyError.E000063);
        }
        checkPermission(bugPO.getProjectId(), DEL_ERROR);
        bugService.delete(id);
        return new JsonResult(1, null);
    }

}
