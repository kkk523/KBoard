package com.lec.spring.service;

import com.lec.spring.domain.Attachment;
import com.lec.spring.domain.Post;
import com.lec.spring.domain.User;
import com.lec.spring.repository.AttachmentRepository;
import com.lec.spring.repository.PostRepository;
import com.lec.spring.repository.UserRepository;
import com.lec.spring.util.U;
import jakarta.servlet.http.HttpSession;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Service
public class BoardServiceImpl implements BoardService {

    @Value("${app.upload.path}")
    private String uploadDir;

    @Value("${app.pagination.write_pages}")
    private int WRITE_PAGES;

    @Value("${app.pagination.page_rows}")
    private int PAGE_ROWS;


    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;

    public BoardServiceImpl(SqlSession sqlSession) {
        postRepository = sqlSession.getMapper(PostRepository.class);
        userRepository = sqlSession.getMapper(UserRepository.class);
        attachmentRepository = sqlSession.getMapper(AttachmentRepository.class);
        System.out.println("💛BoardService() 생성");
    }

    @Override
    public int write(Post post, Map<String, MultipartFile> files) {
        // 현재 로그인한 작성자 정보
        User user = U.getLoggedUser();

        // 위 정보는 session 의 정보이고, 디시 DB 에서 읽어온다.
        user = userRepository.findById(user.getId());
        post.setUser(user);  // 글 작성자 세팅.

        int cnt = postRepository.save(post);   // 글 먼저 저장 (그래야 AI 된 PK값(id) 를 받아온다.

        // 첨부파일 추가.
        addFiles(files, post.getId());

        return cnt;
    }

    // 특정 글(id)  에 첨부파일(들) (files) 추가
    private void addFiles(Map<String, MultipartFile> files, Long id) {
        if(files == null) return;

        for(Map.Entry<String, MultipartFile> e : files.entrySet()){
            // name="upfile##" 인 경우만 첨부파일 등록. (이유, 다른 웹에디터와 섞이지 않도록... ex: summernote)
            if(!e.getKey().startsWith("upfile")) continue;

            // 첨부파일 정보 출력
            System.out.println("\n🏈첨부파일 정보: " + e.getKey());    // name = 값
            U.printFileInfo(e.getValue());   // MultipartFile 정보
            System.out.println();

            // 물리적인 파일 저장
            Attachment file = upload(e.getValue());

            // 성공하면 DB 에도 저장
            if(file != null){
                file.setPost_id(id);   // FK 설정
                attachmentRepository.save(file);   // INSERT
            }

        }
    }

    // 물리적으로 서버에 파일 저장.  중복된 파일 이름 -> rename 처리.
    private Attachment upload(MultipartFile multipartFile) {
        Attachment attachment = null;

        // 담긴 파일이 없으면 pass
        String originalFilename = multipartFile.getOriginalFilename();
        if(originalFilename == null || originalFilename.isEmpty()) return null;

        // 원본 파일명
        String sourceName = StringUtils.cleanPath(originalFilename);

        // 저장할 파일 명
        String fileName = sourceName;

        // 파일이 중복되는지 확인
        File file = new File(uploadDir, fileName);
        if(file.exists()){  // 이미 존재하는 파일명, 중복된다면 다른 이름으로 변경하여 저장.
            // a.txt => a_2378142783946.txt  : time stamp 값을 활용할거다!
            // "a" => "a_2378142783946"  : 확장자 없는 경우

            int pos = fileName.lastIndexOf(".");
            if(pos > -1){  // 확장자가 있는 경우
                String name = fileName.substring(0, pos);  // 파일 '이름'
                String ext = fileName.substring(pos);  // 파일 '.확장자'
                // 중복방지를 위한 새로운 이름
                fileName = name + "_" + System.currentTimeMillis() + ext;
            } else { // 확장자가 없는 파일의 경우.
                fileName += "_" + System.currentTimeMillis();
            }
        }
        // 저장될 파일명
        System.out.println("\tfileName = " + fileName);

        // java.io.*  => java.nio.*
        Path copyOfLocation = Paths.get(new File(uploadDir, fileName).getAbsolutePath());
        System.out.println("\t" + copyOfLocation);

        try {
            Files.copy(
                    multipartFile.getInputStream(),
                    copyOfLocation,
                    StandardCopyOption.REPLACE_EXISTING   // 기본에 존재하면 덮어쓰기.
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        attachment = Attachment.builder()
                .filename(fileName)  // 저장된 이름
                .sourcename(sourceName)  // 원본 이름.
                .build();

        return attachment;
    }

    // 특정 id 의 글 조회
    // 트랜잭션 처리
    // 1. 조회수 증가 (UPDATE)
    // 2. 글 읽어오기 (SELECT)
    @Override
    @Transactional  // <- 이 메소드를 트랜잭션 처리.
    public Post detail(Long id) {
        postRepository.incViewCnt(id);  // UPDATE
        Post post = postRepository.findById(id); // SELECT

        if(post != null){
            // 첨부파일(들) 정보 가져오기
            List<Attachment> fileList = attachmentRepository.findByPost(post.getId());
            setImage(fileList);  // '이미지 파일 여부' 세팅
            post.setFileList(fileList);
        }

        return post;
    }

    // [이미지 파일 여부 세팅]
    private void setImage(List<Attachment> fileList) {
        // upload 실제 물리적인 경로
        String realPath = new File(uploadDir).getAbsolutePath();

        for(Attachment attachment : fileList){
            BufferedImage imgData = null;
            File f = new File(realPath, attachment.getFilename());  // 저장된 첨부파일에 대한 File 객체
            try {
                imgData = ImageIO.read(f);
                // ※ ↑ 파일이 존재 하지 않으면 IOExcepion 발생한다
                //   ↑ 이미지가 아닌 경우는 null 리턴
            } catch (IOException e) {
                System.out.println("파일 존재안함: " + f.getAbsolutePath() + " [" + e.getMessage() + "]");
            }

            if(imgData != null) attachment.setImage(true);   // 이미지 여부 체크
        }

    }

    @Override
    public List<Post> list() {
        return postRepository.findAll();
    }

    // 페이징 리스트
    // page : 현재 페이지 (1-base)
    @Override
    public List<Post> list(Integer page, Model model) {
        // 현재 페이지 , 디폴트는 1
        if(page == null) page = 1;
        if(page < 1) page = 1;

        // 페이징
        // writePages: 한 [페이징] 당 몇개의 페이지가 표시되나
        // pageRows: 한 '페이지'에 몇개의 글을 리스트 할것인가?
        HttpSession session = U.getSession();
        Integer writePages = (Integer)session.getAttribute("writePages");
        if(writePages == null) writePages = WRITE_PAGES;   // 만약 session 에 없으면 기본값으로 동작
        Integer pageRows = (Integer)session.getAttribute("pageRows");
        if(pageRows == null) pageRows = PAGE_ROWS;   // session 에 없으면 기본값으로
        session.setAttribute("page", page);   // 현재 페이지 번호 -> session 에 저장

        long cnt = postRepository.countAll(); // 글 목록 전체의 개수
        int totalPage = (int)Math.ceil(cnt / (double)pageRows);  // 총 몇 '페이지' 분량인가

        // [페이징] 에 표시할 '시작페이지' 와 '마지막페이지'
        int startPage = 0;
        int endPage = 0;

        // 해당 '페이지'의 글 목록
        List<Post> list = null;

        if(cnt > 0){
            // page 값 보정
            if(page > totalPage) page = totalPage;

            // 몇번째 데이터부터 fromRow
            int fromRow = (page - 1) * pageRows;

            // [페이징] 에 표시할 '시작페이지' 와 '마지막페이지' 계산
            startPage = (((page - 1) / writePages) * writePages) + 1;
            endPage = startPage + writePages - 1;
            if (endPage >= totalPage) endPage = totalPage;

            // 해당 page 의 글 목록 읽어오기
            list = postRepository.selectFromRow(fromRow, pageRows);
            model.addAttribute("list", list);
        } else {
            page = 0;
        }

        model.addAttribute("cnt", cnt);  // 전체 글 개수
        model.addAttribute("page", page); // 현재 페이지
        model.addAttribute("totalPage", totalPage);  // 총 '페이지' 수
        model.addAttribute("pageRows", pageRows);  // 한 '페이지' 에 표시할 글 개수

        // [페이징]
        model.addAttribute("url", U.getRequest().getRequestURI());  // 목록 url
        model.addAttribute("writePages", writePages); // [페이징] 에 표시할 숫자 개수
        model.addAttribute("startPage", startPage);  // [페이징] 에 표시할 시작 페이지
        model.addAttribute("endPage", endPage);   // [페이징] 에 표시할 마지막 페이지


        return list;
    }

    @Override
    public Post selectById(Long id) {

        Post post = postRepository.findById(id);

        if(post != null){
            // 첨부파일(들) 정보 가져오기
            List<Attachment> fileList = attachmentRepository.findByPost(post.getId());
            setImage(fileList);  // '이미지 파일 여부' 세팅
            post.setFileList(fileList);
        }

        return post;
    }

    @Override
    public int update(Post post, Map<String, MultipartFile> files, Long[] delfile) {
        int result = 0;
        result = postRepository.update(post);

        // 새로운 첨부파일 추가
        addFiles(files, post.getId());

        // 삭제할 기존의 첨부파일들 삭제
        if(delfile != null){
            for(Long fileId : delfile){
                Attachment file = attachmentRepository.findById(fileId);
                if(file != null){
                    delFile(file);   // 물리적으로 삭제
                    attachmentRepository.delete(file);  // DB에서 삭제
                }
            }
        }


        return result;
    }

    // 특정 첨부파일을 물리적으로 삭제
    private void delFile(Attachment file) {
        String saveDirectory = new File(uploadDir).getAbsolutePath();

        File f = new File(saveDirectory, file.getFilename());
        System.out.println("삭제시도 --> " + f.getAbsolutePath());
        if(f.exists()){
            if(f.delete()) // 삭제
                System.out.println("삭제 성공");
            else
                System.out.println("삭제 실패");
        } else {
            System.out.println("파일이 존재하지 않습니까.");
        }
    }

    @Override
    public int deleteById(Long id) {
        int result = 0;
        Post post = postRepository.findById(id); // 존재하는 데이터인지 읽어오기
        if(post != null){
            // 물리적으로 저장된 첨부파일(들) 삭제
            List<Attachment> fileList = attachmentRepository.findByPost(id);
            if(fileList != null){
                for(Attachment attachment : fileList){
                    delFile(attachment);
                }
            }

            // 글 삭제 (참조하는 첨부파일, 댓글 등도 같이 삭제된다 ON DELETE CASCADE)
            result = postRepository.delete(post);
        }
        return result;
    }
}











