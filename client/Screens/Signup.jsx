import React, {useState,  useRef } from 'react';
import {View, Text, StatusBar,StyleSheet, Image,TouchableOpacity, TextInput,ScrollView} from 'react-native';
import AuroraButton from '../Components/AuroraButton';
import Feather from 'react-native-vector-icons/Feather';
import { Overlay } from 'react-native-elements';
import Axios from 'axios';

const SignUp = ({navigation})=>{

    const [id,setId] = useState('');
    const [password,setPassword] = useState('');
    const [name,setName] = useState('');

    const Signupuser = async() => {
        await Axios.get(`http://10.10.1.58:4000/api/userinsert`,{
        params: {
            'id':id,
            'password':password,
            'name':name
        }
        }).then(res=>{
            window.alert("회원가입이 완료되었습니다. 로그인 해주세요.")
            navigation.navigate("Login")
        })
        .catch(error=> console.log(error));
    }

    return (
        <>
            <View style={styles.body}>
                <ScrollView style={styles.bottomSection} contentContainerStyle={{flexGrow: 1, justifyContent: 'center'}}>                
                    <View style={{justifyContent:'center',alignItems:'center',height:'80%'}}>
                        <View style={{width:'80%',marginBottom:5}}>
                        <Text style={{color:'rgba(255,255,255,0.7)',fontSize:40,marginTop:20,marginBottom:30,fontWeight:"bold"}}>오늘 자격 회원가입 </Text> 
                            <TextInput name="Name" value={name} onChangeText={text => setName(text)} style={styles.input} placeholderTextColor="#fff" placeholder="이름" />  
                            <TextInput name="AccountID" value={id} onChangeText={text => setId(text)} style={styles.input} placeholderTextColor="#fff" placeholder="아이디" /> 
                            <TextInput name="password" value={password} onChangeText={text => setPassword(text)} secureTextEntry={true} style={styles.input} placeholderTextColor="#fff" placeholder="비밀번호" />
                        </View>
                    <View style={styles.rowcontainer}>
                        <AuroraButton buttonFunction={()=>Signupuser()} width="70%" height={50} fontSize={15} bgcolor="rgba(255,255,255,0.7)" fontWeight="bold" text="회원가입 완료" color={"black"} outline={false}/>
                    </View>
                    </View>
                </ScrollView>
            </View>
        </>
    )
}

export default SignUp
const styles = StyleSheet.create({

    topSection:{
      alignItems:'center',
      justifyContent:'center',
      height:'5%',
    },
    input:{
      height:52,
      width:'100%',
      color:'white',
      borderRadius:10,
      borderwidth: "1px",
      borderColor:'rgba(255,255,255,0.7)',
      paddingLeft:10
    },
    body:{
      flex:1,
      backgroundColor:'white',
      flexDirection:'column',
      justifyContent:'space-between'
    },
    bottomSection:{
      width:'100%',
      height:'95%',
      backgroundColor:'rgba(44,117,84,0.8)',
  
    },
    rowcontainer:{
      ...Platform.select({
        native:{
          justifyContent: 'center', 
          alignItems: 'center',
          width:'70%',
          height:'30%',
        },
        default:{
          justifyContent: 'center', 
          alignItems: 'center',
          width:'40%',
          height:'40%',
          flexDirection:"row",
        }, 
      }), 
    },
  })