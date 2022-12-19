
import React, {useState,useEffect} from 'react';
import { useNavigation } from '@react-navigation/native';
import { View,Text,StyleSheet,ScrollView } from "react-native";
import { TouchableOpacity } from 'react-native';
import Axios from "axios";

const Test = ({userinfo})=>{

    const [list,setList]=useState([]);
    const navigation = useNavigation();

    const testlist = async() => {
        await Axios.get(`http://10.10.1.58:4000/api/alltest`,{
        }).then(res=>{
            const testlists = []
            res.data.data.map((testsubject) =>{
                testlists.push(testsubject);
            })
            setList(testlists);
        })
        .catch(error=> console.log(error));
      }

    async function exam(subject){
        navigation.navigate("TestDetails", {
            subject:subject
        })
      }

    useEffect(() => {
        testlist()
      }, []);

    return(
        <ScrollView>
           {list.map(test=>
                <>
                <View style={styles.box}>
                    <TouchableOpacity onPress={() => exam(test.subject)}>
                    <Text style={{fontSize:20,fontWeight:"bold"}}>{test.subject}</Text>
                    <View style={styles.row}>
                        <Text style={{fontSize:16,fontWeight:"bold"}}>문제 </Text>
                        <Text style={{fontSize:16,fontWeight:"bold",color:"rgba(44,117,84,0.8)"}}>10</Text>
                    </View>
                    </TouchableOpacity>
                </View>
                </>
             )}
            
        </ScrollView>
    )
}

export default Test

const styles = StyleSheet.create({
    box:{
        margin:'5%',
        padding:'3%',
        width:'90%',
        height:100,
        backgroundColor:'lightgrey',
        borderRadius:10,
    },
    row:{
        justifyContent: 'center', 
        alignItems: 'center',
        width:'40%',
        height:'40%',
        flexDirection:"row", 
    }
})
