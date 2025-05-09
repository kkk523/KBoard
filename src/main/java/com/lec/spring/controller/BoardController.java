package com.lec.spring.controller;

import com.lec.spring.domain.Post;
import com.lec.spring.domain.PostValidator;
import com.lec.spring.service.BoardService;
import com.lec.spring.util.U;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

// Controller layer
//  request 처리 ~ response
@Controller
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        System.out.println("🧡BoardController() 생성");
        this.boardService = boardService;
    }


    @GetMapping("/write")
    public void write(){}

    @PostMapping("/write")
    public String writeOk(
            @RequestParam Map<String, MultipartFile> files,   // 첨부파일들 <name, file>
            @Valid Post post,
            BindingResult result,  // Validator 가 유효성 검사를 한 결과가 담긴 객체.
            Model model, // 매개변수 선언시 BindingResult 보다 Model 을 뒤에 두어야 한다.
            RedirectAttributes redirectAttributes  // redirect: 시 넘겨줄 값들.
    ){
        // validation 에러가 있었다면 redirect 한다!
        if(result.hasErrors()){
            showErrors(result);

            // addAttribute(name, value)
            //    request parameters로 값을 전달.  redirect URL에 query string 으로 값이 담김
            //    request.getParameter에서 해당 값에 액세스 가능
            // addFlashAttribute(name, value)
            //    일회성. 한번 사용하면 Redirect후 값이 소멸
            //    request parameters로 값을 전달하지않음
            //    '객체'로 값을 그대로 전달

            // redirect 시, 기존에 입력했던 값들은 보이도록 전달해주어야 한다
            //   전달한 name 들은 => 템플릿에서 사용가능한 변수!
            redirectAttributes.addFlashAttribute("user", post.getUser());
            redirectAttributes.addFlashAttribute("subject", post.getSubject());
            redirectAttributes.addFlashAttribute("content", post.getContent());

            // 어떤 에러가 발생했는지 정보도 전달
            for(FieldError err : result.getFieldErrors()){
                redirectAttributes.addFlashAttribute("error_" + err.getField(), err.getCode());
                                               //  "error_user" ,
                                               //  "error_subject"
            }



            return "redirect:/board/write";  // GET
        }

        model.addAttribute("result", boardService.write(post, files));
        return "board/writeOk";  // view
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model){
        model.addAttribute("post", boardService.detail(id));
        return "board/detail";
    }

    @GetMapping("/list")
    public void list(Integer page, Model model){
//        model.addAttribute("list", boardService.list());
        boardService.list(page, model);
    }

    @GetMapping("/update/{id}")
    public String update(@PathVariable Long id, Model model){
        model.addAttribute("post", boardService.selectById(id));
        return "board/update";
    }

    @PostMapping("/update")
    public String updateOk(
            @RequestParam Map<String, MultipartFile> files,   // 새로 추가될 첨부파일들
            Long[] delfile,     // 삭제될 파읻들의 id(들)
            @Valid Post post
            , BindingResult result
            , Model model
            , RedirectAttributes redirectAttributes
    ){
        if(result.hasErrors()){
            showErrors(result);

            redirectAttributes.addFlashAttribute("subject", post.getSubject());
            redirectAttributes.addFlashAttribute("content", post.getContent());
            for(FieldError err : result.getFieldErrors()){
                redirectAttributes.addFlashAttribute("error_" + err.getField(), err.getCode());
            }

            return "redirect:/board/update/" + post.getId();
        }


        model.addAttribute("result", boardService.update(post, files, delfile));  // <- id, subject, content
        return "board/updateOk";
    }

    @PostMapping("/delete")
    public String delete(Long id, Model model){
        model.addAttribute("result", boardService.deleteById(id));
        return "board/deleteOk";
    }

    // 바인딩 에러 출력 도우미 메소드
    public void showErrors(Errors errors){
        if(errors.hasErrors()){
            System.out.println("💢에러개수: " + errors.getErrorCount());
            // 어떤 field 에 어떤 에러(code) 가 담겨있는지 확인
            System.out.println("\t[field]\t|[code]");
            List<FieldError> errList = errors.getFieldErrors();
            for(FieldError err : errList){
                System.out.println("\t" + err.getField() + "\t|" + err.getCode());
            }
        } else {
            System.out.println("✔에러 없슴");
        }
    } // end showErrors()

    @InitBinder
    public void initBinder(WebDataBinder binder){
        System.out.println("🐹@InitBinder 호출");
        binder.setValidator(new PostValidator());
    }


    // 페이징
    // pageRows 변경시 동작
    @PostMapping("/pageRows")
    public String pageRows(Integer page, Integer pageRows){
        U.getSession().setAttribute("pageRows", pageRows);
        return "redirect:/board/list?page=" + page;
    }

}















