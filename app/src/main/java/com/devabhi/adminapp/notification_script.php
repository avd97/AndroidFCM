<?php

    function sendNotif ($to, $notif) {

        $apiKey = "AAAAZj5UN4U:APA91bFWdYMcKCFwXNr29-7vxCBY8EfEOKeFISRUkxlTwsxlwNKY1qE50BCcL_auFczchPe-8ZCxrQhmIBRR5gWzTtpeHSsKb_t6eP_onmCONK0YdZBQ1f73V-bXN66PRyYX5hVOaUlz";

        $feilds = json_encode(array('to'=>$to, 'notification'=>$notif));

        $ch = curl_init();

        $url = 'https://fcm.googleapis.com/fcm/send';

        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $feilds);

        $headers = array();
        $headers[] = 'Authorization: key='.$apiKey;
        $headers[] = 'Content-Type: application/json';
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        $result = curl_exec($ch);
        if (curl_errno($ch)) {
            echo 'Error:' . curl_error($ch);
        }
        curl_close($ch);
    }

    $to = "/topics/newsletter";
    // $to = "";

    $notification = array(
        'title' => "New Messages",
        'body' => "from News Letter"
    );

    sendNotif($to, $notification);

?>