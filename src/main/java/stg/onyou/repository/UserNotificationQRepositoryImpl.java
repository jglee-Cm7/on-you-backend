package stg.onyou.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import stg.onyou.exception.CustomException;
import stg.onyou.exception.ErrorCode;
import stg.onyou.model.entity.UserNotification;
import stg.onyou.model.network.response.ClubNotificationResponse;
import stg.onyou.model.network.response.QUserNotificationResponse;
import stg.onyou.model.network.response.UserNotificationResponse;

import java.util.List;
import java.util.Optional;

import static stg.onyou.model.entity.QUser.user;
import static stg.onyou.model.entity.QUserNotification.userNotification;
import static stg.onyou.model.entity.QAction.action;

@Repository
public class UserNotificationQRepositoryImpl extends QuerydslRepositorySupport implements UserNotificationQRepository{

    private final JPAQueryFactory queryFactory;

    @Autowired
    public UserNotificationQRepositoryImpl(JPAQueryFactory queryFactory) {
        super(UserNotification.class);
        this.queryFactory = queryFactory;
    }

    @Override
    public List<UserNotificationResponse> findUserNotificationList(Long userId) {

        List<UserNotificationResponse> notificationResponseList = queryFactory
                .select(new QUserNotificationResponse(
                    action.actioner.id,
                    action.actionee.id,
                    action.actionClub.id,
                    action.actionType,
                    action.applyMessage
                ))
                .from(userNotification)
                .innerJoin(userNotification.action, action)
                .where(userNotification.recipient.id.eq(userId))
                .fetch();


        for(UserNotificationResponse notification : notificationResponseList){

            String actioneeName = "";
            if(notification.getActioneeId()!=null){
                actioneeName = queryFactory.select(user.name).from(user).where(user.id.eq(notification.getActioneeId()))
                        .fetchOne();
            }

            notification.setActioneeName(actioneeName);

            String actionerName = queryFactory.select(user.name).from(user).where(user.id.eq(notification.getActionerId()))
                    .fetchOne();
            notification.setActionerName(actionerName);
        }

        return notificationResponseList;
    }

}
