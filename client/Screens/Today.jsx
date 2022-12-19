import { StyleSheet, Text, View, TextInput,KeyboardAvoidingView,TouchableOpacity, ScrollView} from 'react-native';
import { useState, useEffect } from 'react';
import { useNavigation } from '@react-navigation/native';
import Ionicons from 'react-native-vector-icons/Ionicons';
import Axios from "axios";

const Today = ({userinfo})=>{

    const navigation = useNavigation();

    const[certificate,setCertificate]= useState('');
    const [list,setList]=useState([]);

    const certificatelist = async() => {
        await Axios.get(`http://10.10.1.58:4000/api/allcertificateinfo`,{
        }).then(res=>{
            const certificatelists = []
            res.data.data.map((certificate) =>{
                certificatelists.push(certificate);
            })
            setList(certificatelists);
        })
        .catch(error=> console.log(error));
      }

    useEffect(() => {
        certificatelist()
      }, []);

    async function exam(subject){
    navigation.navigate("TestDetails", {
        subject:subject
    })
    }


    return(
        <>
        <View style={styles.home}>
            <View style={styles.row}>
            <TextInput  style={styles.input} type="text" placeholder="자격증 정보 검색" value={certificate} name="certificate" />
            <TouchableOpacity >
                <Ionicons name="search-outline" size={35} color={"black"}/>
            </TouchableOpacity>
            </View>
            <ScrollView style={styles.bottomhome}>
            {list.map(certificate=>
                    <>
                    <View style={styles.box}>
                        <TouchableOpacity onPress={() => exam(certificate.certificate_name)}>
                        <Text style={{fontSize:20,fontWeight:"bold"}}>{certificate.category}</Text>
                        <Text style={{fontSize:20,fontWeight:"bold"}}>{certificate.certificate_name}</Text>
                        <Text style={{fontSize:16,fontWeight:"bold",color:"rgba(44,117,84,0.8)"}}>{certificate.information}</Text>
                        </TouchableOpacity>
                    </View>
                    </>
                )}
             </ScrollView>
        <View>
        </View>
        </View>
        </>
    )
}

export default Today

const styles = StyleSheet.create({
    box:{
        margin:'5%',
        padding:'3%',
        width:'90%',
        height:150,
        backgroundColor:'lightgrey',
        borderRadius:10,
    },
    home:{
      alignItems: "center",
      justifyContent: "center",
      width:"100%",
      height:"100%"
    },
    bottomhome:{
        width:"100%",
        marginTop:"20%",

      },
    Textsize1:{
      fontSize:40,
      color:"white",
      fontWeight: 'bold'
    },
    Textsize2:{
      fontSize:18,
      marginBottom:"3%",
      color:"black",
      fontWeight: 'bold'
    },
    Textsize3:{
      marginTop:"10%",
      marginBottom:"5%",
      fontSize:20,
      justifyContent:"flex-start",
      alignItems:"flex-start",
      color:"black",
      fontWeight: 'bold'
    },
    alarm:{
      width:"90%",
      height:130,
      borderBottomWidth: 2,
      padding: 10,
  },
    input: {
      backgroundColor:"lightgrey",
      borderBottomWidth: 2,
      borderStyle: 'solid',
      width:"80%",
      height:40,
      marginRight:10,
      padding: 10,
      borderRadius:10,
      marginBottom:10,
    },
    row:{ 
      position:"absolute",
      top:"5%",
      width:"80%",
      flexDirection:"row",
      flexWrap: "wrap",
      alignItems: "center",
      justifyContent: "center",
    },
  });