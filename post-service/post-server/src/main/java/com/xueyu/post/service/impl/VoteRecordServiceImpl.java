package com.xueyu.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyu.post.exception.PostException;
import com.xueyu.post.mapper.VoteMapper;
import com.xueyu.post.mapper.VoteOptionMapper;
import com.xueyu.post.mapper.VoteRecordMapper;
import com.xueyu.post.pojo.domain.Vote;
import com.xueyu.post.pojo.domain.VoteCycle;
import com.xueyu.post.pojo.domain.VoteRecord;
import com.xueyu.post.pojo.domain.VoteType;
import com.xueyu.post.service.VoteRecordService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class VoteRecordServiceImpl extends ServiceImpl<VoteRecordMapper, VoteRecord> implements VoteRecordService {

    @Resource
    VoteRecordMapper voteRecordMapper;

    @Resource
    VoteMapper voteMapper;

    @Resource
    VoteOptionMapper voteOptionMapper;

    @Override
    public boolean addVoteRecord(Integer userId, Integer voteId , Integer[] optionIds) {
        //判断投票是否存在
        Vote vote = voteMapper.selectById(voteId);
        if(vote==null){
            throw new PostException("该投票不存在");
        }
        //判断投票是否过期
        if(!isVoteExpired(vote)){
            throw new PostException("该投票已过期");
        }
        //判断选项个数是否为0
        if(optionIds==null){
            throw new PostException("没有添加选项");
        }
        //判断投票为单选还是多选
        if(vote.getType().equals(VoteType.RADIO.getValue())){
            //单选
            if(optionIds.length>1){
                throw new PostException("该投票只能单选");
            }
            VoteRecord voteRecord = new VoteRecord();
            voteRecord.setUserId(userId);
            voteRecord.setOptionId(optionIds[0]);
            voteRecord.setVoteId(voteId);
            voteRecordMapper.insert(voteRecord);
            //投票选项人数加一
            voteOptionMapper.addVoteOptionNum(optionIds[0]);
            //投票总人数加一
            voteMapper.addVoteAmount(voteId);
        }else {
            //多选
            VoteRecord voteRecord;
            for (Integer optionId : optionIds){
                voteRecord = new VoteRecord();
                voteRecord.setUserId(userId);
                voteRecord.setOptionId(optionId);
                voteRecord.setVoteId(voteId);
                voteRecordMapper.insert(voteRecord);
                //投票选项人数加一
                voteOptionMapper.addVoteOptionNum(optionId);
            }
            //投票总人数加一
            voteMapper.addVoteAmount(voteId);
        }
        return true;
    }

    @Override
    public List<Integer> isVote(Integer userId, Integer voteId) {
        //判断投票是否存在
        Vote vote = voteMapper.selectById(voteId);
        if(vote==null){
            throw new PostException("该投票不存在");
        }
        LambdaQueryWrapper<VoteRecord> recordQueryWrapper = new LambdaQueryWrapper<>();
        recordQueryWrapper.eq(VoteRecord::getUserId,userId)
                .eq(VoteRecord::getVoteId,voteId);
        List<VoteRecord> list = voteRecordMapper.selectList(recordQueryWrapper);
        List<Integer> optionIds = new ArrayList<>();
        //获得用户点赞了哪些选项
        for(VoteRecord voteRecord : list){
            optionIds.add(voteRecord.getOptionId());
        }
        if(optionIds.isEmpty()){
            optionIds = null;
        }
        return optionIds;
    }

    @Override
    public Boolean isVoteExpired(Vote vote){
        //判断投票是否过期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(vote.getCreateTime());
        if(vote.getCycle().equals(VoteCycle.DAY.getValue())){
            calendar.add(Calendar.DATE,1);
        } else if(vote.getCycle().equals(VoteCycle.WEEK.getValue())){
            calendar.add(Calendar.WEEK_OF_MONTH,1);
        } else if(vote.getCycle().equals(VoteCycle.MONTH.getValue())){
            calendar.add(Calendar.MONTH,1);
        } else if(vote.getCycle().equals(VoteCycle.YEAR.getValue())){
            calendar.add(Calendar.YEAR,1);
        }
        Timestamp endTime = new Timestamp(calendar.getTimeInMillis());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.before(endTime);
    }
}
