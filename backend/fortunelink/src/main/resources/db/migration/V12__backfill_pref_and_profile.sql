-- backfill_user_preferences

INSERT INTO user_preferences (user_id,
                              base_currency,
                              email_notifications,
                              price_alerts,
                              date_format)
SELECT id,
       'CAD',
       TRUE,
       TRUE,
       'MM/DD/YYYY'
FROM users
WHERE id NOT IN (SELECT user_id FROM user_preferences);

-- backfill_user_profile

INSERT INTO user_profile (user_id,
                          full_name)
SELECT id,
       COALESCE(email, '')
FROM users
WHERE id NOT IN (SELECT user_id FROM user_profile);