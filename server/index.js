
const express = require('express');
const app = express();
const PORT = process.env.PORT || 4000;
const cors = require("cors");

var corsOptions = {
    origin: "http://localhost:19000" //클라이언트(react) 쪽의 콜스 허용
};


const mysql = require('mysql');
const connection = mysql.createConnection({
    host : 'localhost',
    user : 'root',
    password : 'wlrbdls99!', 
    database : 'Todaycertificate' 
});
connection.connect((err) => {
    if (err) {
        console.log('error connecting: ' + err.stack);
        return;
    }
    console.log('success');
});

app.use(cors(corsOptions));

app.get('/', (req, res) => {
    res.send('Server Response Success');
})


app.get('/api/userinfo',(req,res)=>{
    const sql= "select * from Member where `id`= ? "
    const params = [req.query.id]
    connection.query(sql,params,(err,data) =>{
        if(!err) res.send({ data : data });
        else res.send(err);
    })
})


app.get('/api/userinsert', (req, res) => {
    const id=req.query.id;
    const name=req.query.name;
    const password=req.query.password;
    const sql= "INSERT INTO Member (id,name,acquisition_certificate,password) VALUES (?,?,NULL ,?)"
    connection.query(sql,[id,name,password],(err,data) =>{
        if(!err) res.send({ data : data });
        else res.send(err);
    })
})

app.get('/api/userupdate', (req, res) => {
    const id=req.query.id;
    const name=req.query.name;
    const password=req.query.password;
    const acquisition_certificate=req.query.acquisition_certificate;
    const sql= "UPDATE Member SET name = ?, acquisition_certificate = ?, password = ? WHERE id= ?;"
    connection.query(sql,[name,acquisition_certificate,password,id],(err,data) =>{
        if(!err) res.send({ data : data });
        else res.send(err);
    })
})


app.get('/api/alluserinfo', (req, res) => {
    connection.query("SELECT * FROM Member", (err, data) => {
        if(!err) res.send({ data : data });
        else res.send(err);
    })
});


app.get('/api/testinfo',(req,res)=>{
    const sql= "select * from Question where `subject`= ? "
    const params = [req.query.subject]
    connection.query(sql,params,(err,data) =>{
        if(!err) res.send({ data : data });
        else res.send(err);
    })
})

app.get('/api/alltest', (req, res) => {
    const sql="select DISTINCT subject FROM Question;";
    connection.query(sql,(err,data) => {
        if(!err) res.send({ data : data });
        else res.send(err);
    })
});

app.get('/api/reviewinsert', (req, res) => {
    const review=req.query.review;
    const id=req.query.id;
    const sql= "UPDATE Member SET acquisition_certificate_review=? WHERE id= ?;"
    connection.query(sql,[review,id],(err,data) =>{
        if(!err) res.send({ data : data });
        else res.send(err);
    })
});


app.get('/api/allcertificateinfo', (req, res) => {
    connection.query("SELECT * FROM Certificate", (err, data) => {
        if(!err) res.send({ data : data });
        else res.send(err);
    })
});

app.get('/api/allanswer', (req, res) => {
    connection.query("SELECT * FROM Answer", (err, data) => {
        if(!err) res.send({ data : data });
        else res.send(err);
    })
});
app.listen(PORT, () => {
    console.log(`Server On : http://localhost:${PORT}/`);
})