package com.xueyu.user.service.impl;

import com.xueyu.resource.client.ResourceClient;
import com.xueyu.user.exception.UserException;
import com.xueyu.user.mapper.UserMapper;
import com.xueyu.user.mapper.UserViewMapper;
import com.xueyu.user.pojo.domain.User;
import com.xueyu.user.pojo.vo.UserView;
import com.xueyu.user.service.PersonCenterService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author durance
 */
@Service
public class PersonCenterServiceImpl implements PersonCenterService {

	@Resource
	UserMapper userMapper;

	@Resource
	UserViewMapper userViewMapper;

	@Resource
	ResourceClient resourceClient;

	@Override
	public Boolean updateUserInfo(User user) {
		if (user.getPassword() != null || user.getAvatar() != null || user.getUsername() != null || user.getCreateTime() != null || user.getOpenid() != null) {
			throw new UserException("不合法的参数传入");
		}
		int i = userMapper.updateById(user);
		if (i != 1) {
			throw new UserException("不存在的用户id");
		}
		return true;
	}

	@Override
	public String updateUserAvatar(Integer userId, MultipartFile file) {
		User check = userMapper.selectById(userId);
		if (check == null) {
			throw new UserException("不存在的用户id");
		}
		// 如果不为默认头像，删除原来的头像文件
		String originName = check.getAvatar();
		if (!"default.jpg".equals(originName)) {
			resourceClient.deleteFileByFileName(originName);
		}
		// 进行头像保存，获取文件名
		Map<String, String> resDate = resourceClient.uploadImageFile(file).getData();
		String fileName = resDate.get("fileName");
		User user = new User();
		user.setId(userId);
		user.setAvatar(fileName);
		userMapper.updateById(user);
		UserView userView = userViewMapper.selectById(userId);
		return userView.getAvatarUrl();
	}

}
