package team.magic.flute.hercules.common.http;


/**
 * 2021-12-27  CL
 **/
public class BasePageResponse<T extends PageData<?>> extends BaseResponse<T> {

    private T data;

    public BasePageResponse() {
        super();
    }

    public BasePageResponse(T data) {
        super();
        this.data = data;
    }

    public static <T extends PageData<?>> BasePageResponse<T> send(T data) {
        BasePageResponse<T> pageResponse = new BasePageResponse<>();
        EnumResponseType type = EnumResponseType.SUCCESS;
        pageResponse.setCode(type.getCode());
        pageResponse.setMsg(type.getMessage());
        pageResponse.setData(data);
        return pageResponse;
    }

    public static <T extends PageData<?>> BasePageResponse<T> send(EnumResponseType type, T data) {
        BasePageResponse<T> pageResponse = new BasePageResponse<>();
        pageResponse.setCode(type.getCode());
        pageResponse.setMsg(type.getMessage());
        pageResponse.setData(data);
        return pageResponse;
    }

    public static <T extends PageData<?>> BasePageResponse<T> send(Throwable throwable, T data) {
        BasePageResponse<T> pageResponse = new BasePageResponse<>();
        pageResponse.setCode(EnumResponseType.DEFAULT_ERROR.getCode());
        pageResponse.setMsg(throwable.getMessage());
        pageResponse.setData(data);
        return pageResponse;
    }

    @Override
    public T getData() {
        return data;
    }


    @Override
    public void setData(T data) {
        this.data = data;
    }
}

