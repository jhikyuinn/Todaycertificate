import React, {useState,useEffect} from 'react';
import Axios from "axios"
import {Platform, View, Text,StyleSheet, TextInput, ScrollView,TouchableOpacity} from 'react-native';
import AuroraButton from '../Components/AuroraButton';
import Ionicons from 'react-native-vector-icons/Ionicons';

const TestDetails = (subject)=>{

    const [infolist,setInfoList]=useState([]);
    const [answerlist,setAnswerList]=useState([]);
    const Testsubject=subject.route.params.subject;

    const testquestionlist = async() => {
        await Axios.get(`http://10.10.1.58:4000/api/testinfo`,{
            params:{
                subject:Testsubject
            }
        }).then(res=>{
            const testinfolists = []
            res.data.data.map((data)=>{
                testinfolists.push(data);
            })
            setInfoList(testinfolists);
        })
        .catch(error=> console.log(error));
    }

    const testanswerlist = async() => {
        await Axios.get(`http://192.168.0.19:4000/api/allanswer`,{
        }).then(res=>{
            const testanswerlists = []
            res.data.data.map((data)=>{
                console.log(data)
                testanswerlists.push(data);
            })
            setAnswerList(testanswerlists);
        })
        .catch(error=> console.log(error));
    }


    useEffect(() => {
        testquestionlist()
        testanswerlist()
    }, []);


    return(
        <ScrollView>
            {infolist.map(record=>
                <>
                <View style={styles.box}>
                <View style={styles.row}>
                <Text style={{fontSize:16,fontWeight:"bold"}}>문제 {record.question_code}</Text>
                    <TouchableOpacity style={{marginLeft:"10%"}} >
                        <Ionicons name="heart-outline" size={25}/>
                    </TouchableOpacity>
                    </View>
                <Text style={{fontSize:20,fontWeight:"bold"}}>{record.question}</Text>

                <View style={styles.row}>
                    <AuroraButton  width="45%" height={50} bgcolor="rgba(255,255,255,0.7)" fontSize={15} fontWeight="bold" text="정답 보기" color={"black"} outline={false}/>
                    <AuroraButton  width="45%" height={50} bgcolor="rgba(255,255,255,0.7)" fontSize={15} fontWeight="bold" text="해설 보기" color={"black"} outline={false}/>
                </View>
                <Text style={{fontSize:13,marginTop:"7%",fontWeight:"bold"}}>{record.answer}</Text>
                </View>
                </>
            )}
            {answerlist.map(answer=>
                <View>
                <Text style={{fontSize:13,marginTop:"7%",fontWeight:"bold"}}>{answer.explanation}</Text>
                </View>
            )}
        </ScrollView>
    )
}

export default TestDetails;

const styles = StyleSheet.create({
    box:{
        margin:'5%',
        padding:'3%',
        width:'90%',
        height:300,
        backgroundColor:'lightgrey',
        borderRadius:10,
    },
    row:{
        width:'40%',
        height:'15%',
        flexDirection:"row", 
    }
})
