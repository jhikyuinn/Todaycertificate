
import React, {useState,useEffect} from 'react';
import Axios from "axios"
import {Platform, View, Text,StyleSheet, TextInput, ScrollView} from 'react-native';
import AuroraButton from '../Components/AuroraButton';

const Login = ({route,navigation})=>{
  const [id,setId] = useState('');
  const [password,setPassword] = useState('');

  const loginuser = async() => {
    await Axios.get(`http://10.10.1.58:4000/api/userinfo`,{
      params: {
        'id':id,
      }
    }).then(res=>{
      const loginres=Object.values(res.data.data)
      if(JSON.stringify(loginres)!=="[]" && loginres[0].password === password){
        navigation.navigate("Menu", {
          userinfo:loginres
        })
      }
      else{
        window.alert("정보가 다릅니다. 다시 입력해주세요.")
      }  
    })
    .catch(error=> console.log(error));
  }

  const signnavigate = () => {
    navigation.navigate("Signup")
  }

  
  return (
    <>
      <View style={styles.body}>
        <ScrollView style={styles.bottomSection} contentContainerStyle={{flexGrow: 1, justifyContent: 'center'}}>
          <View style={{justifyContent:'center',alignItems:'center',height:'80%'}}>
            <View style={{width:'70%',marginTop:5}}>
                <Text style={{color:'rgba(255,255,255,0.7)',fontSize:40,marginTop:20,marginBottom:30,fontWeight:"bold"}}>오늘 자격{'\n'}</Text> 
                <TextInput value={id} onChangeText={text => setId(text)} style={styles.input} placeholderTextColor="rgba(255,255,255,0.7)" placeholder="아이디" />  
                <TextInput value={password} onChangeText={text => setPassword(text)} secureTextEntry={true} style={styles.input} placeholderTextColor="rgba(255,255,255,0.7)" placeholder="비밀번호" />  
            </View>
            <View style={styles.rowcontainer}>
              <AuroraButton  width="70%" height={50} bgcolor="rgba(255,255,255,0.7)" buttonFunction={loginuser} fontSize={15} fontWeight="bold" text="로그인" color={"black"} outline={false}/>
              <AuroraButton  width="70%" height={50} bgcolor="rgba(255,255,255,0.7)" buttonFunction={signnavigate} fontSize={15} fontWeight="bold" text="회원가입" color={"black"} outline={false}/>
            </View>
          </View>
        </ScrollView>
      </View>
    </>
    )
  }
  
export default Login

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